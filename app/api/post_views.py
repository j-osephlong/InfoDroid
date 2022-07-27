from datetime import datetime, timezone
from http import HTTPStatus
from rest_framework.views import APIView
from rest_framework import permissions, authentication
from rest_framework.response import Response
from rest_framework import status
from api.imgur_requests import delete_image

from api.request_modifiers import author_pk_to_username

from .serializers import PostSerializer
from .models import Post
from django.db.models.query import QuerySet
from django.db.models import F
from django.shortcuts import get_object_or_404

from .distance import haversine

from .request_modifiers import add_image, add_ownership_from_id, add_user_as_author, user_is_user, add_locality, place_id_to_name

class PostView(APIView):
    authentication_classes = (authentication.TokenAuthentication,)
    permission_classes = (permissions.IsAuthenticated,)
    serializer_class = PostSerializer
    
    def post(self, request):
        modified_request = request.data.copy()
        
        if 'coord_longitude' not in modified_request or 'coord_latitude' not in modified_request:
            return Response(
                {"status": "error", "data": "coord_longitude or coord_latitude not provided."},
                status=HTTPStatus.BAD_REQUEST
            )
        
        ### remove these fields from request if present
        modified_request.pop('author', None)
        modified_request.pop('locality', None)
        modified_request.pop('created_on', None)
        modified_request.pop('image', None)
        ###
        
        ### add database items that need a little processing
        modified_request = add_user_as_author(modified_request, request.user)
        modified_request = add_locality(modified_request)
        modified_request = add_image(modified_request) # this function will handle case of no image
        ###
        
        serializer = self.serializer_class(data = modified_request)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        
        ### replace some fields with human readable fields
        modified_data = place_id_to_name(serializer.data)
        ###
        
        return Response(
            {"status": "success", "data": modified_data}
        )
    
    def get(self, request, id=None):
        ##
        # validate location from user request
        ##
        if request.GET.get("lat") == None or request.GET.get("lon") == None:
            return Response (
                {
                    "data": "Must provide both coord_latitude and coord_longitude, and optionally range_meters.",
                    "status": "error"
                },
                status=HTTPStatus.BAD_REQUEST
            )
        location = {
            "coord_longitude": float(request.GET.get("lon")),
            "coord_latitude": float(request.GET.get("lat"))
        }
        ## validate range given by user
        if request.GET.get("range") != None:
            location.update({"range_meters": int(request.GET.get("range"))})
        if "range_meters" in location:
            if location["range_meters"] < 10 or location["range_meters"] > 10000:
                return Response (
                    {
                        "data": "Filter range must be within range [10, 10000] (inclusive).",
                        "status": "error",
                        "code": "rangeRangeError"
                    },
                    status=HTTPStatus.BAD_REQUEST
                )
        
        ## get user's locality data from google API
        modified_request = add_locality(location)
        
        ## route request based on location query type [range | locality]
        if 'range_meters' in location:
            return self.get_by_coords(request, location, id)
        else:
            modified_request = add_locality(location)
                # return error is no_location locality (not enough info to continue query) 
            return self.get_by_locality(request, modified_request, id)

    def get_by_coords(self, request, location, id = None):
        limit = 10 if request.GET.get("limit") == None else int(request.GET.get("limit"))
        page = 0 if request.GET.get("page") == None else int(request.GET.get("page"))
        
        ## request can either be query for list of posts or for single post [id]
        if id == None:
            # filter based off of range, using haversine method 
            posts = Post.objects.annotate(
                    distance = haversine(F('coord_longitude'), F('coord_latitude'), location['coord_longitude'], location['coord_latitude'])
                ).filter(
                    distance__lte=location['range_meters']
                ).filter(
                    distance__lte=F('range_meters')   
                )

            # filter further by post end_time
            posts_active = (posts.filter(end_time__gt = datetime.now(timezone.utc)) | posts.filter(end_time = None)).distinct().order_by('-created_on') [page*limit:page*limit+limit]
            
            #create json serializer
            post_serializer = self.serializer_class(posts_active, many = True)
            
            # human readability modifiers
            modified_data = add_ownership_from_id(post_serializer.data, request.user, many = True)
            modified_data = author_pk_to_username(modified_data, many = True)
            modified_data = place_id_to_name(modified_data, many = True)
            
            return Response({
                "status": "success",
                "data": modified_data
            })
        else:
            post = get_object_or_404(Post, pk = id)
            
            # ensure post is within range of user
            in_range = len(
                Post.objects.filter(
                    pk=id
                ).annotate(
                    distance = haversine(F('coord_longitude'), F('coord_latitude'), location['coord_longitude'], location['coord_latitude'])
                ).filter(
                    distance__lte = location['range_meters']
                ).filter (
                    distance__lte = F('range_meters')
                )
            ) == 1
            
            # if not in range or owned by requesting user, deny access
            if not user_is_user(request.user, post.author.username) and not in_range:
                return Response({
                    "status": "error",
                    "data": f"Post with id={id} outside of ranges (post/privacy range = {post.range_meters}, user/filter range = {location['range_meters']}).",
                    "code" : "rangeError"
                }, status=HTTPStatus.UNAUTHORIZED)
            # create post serializer, annotating distance from user
            post_serializer = self.serializer_class(
                Post.objects.annotate(
                    distance = haversine(F('coord_longitude'), F('coord_latitude'), location['coord_longitude'], location['coord_latitude'])
                ).get(pk = post.id)
            )
            
            # human readability modifiers
            modified_data = add_ownership_from_id(post_serializer.data, request.user)
            modified_data = author_pk_to_username(modified_data)
            modified_data = place_id_to_name(modified_data)
            
            return Response({
                "status": "success",
                "data": modified_data
            })
            
    def get_by_locality(self, request, location, id = None):
        limit = 10 if request.GET.get("limit") == None else int(request.GET.get("limit"))
        page = 0 if request.GET.get("page") == None else int(request.GET.get("page"))
        
        ##
        # at this point locality is just the google_place_id ( a char field ), and no_location has "0" for this
        # if == 0, no available locality, so deny access
        if location['locality'] == "0":
            return Response(
                {
                    "status": "error",
                    "data": "By ommiting a range, locality mode was selected, but no valid locality could be found for the provided locality.",
                    "code" : "invalidLocalityError"
                },
                status=HTTPStatus.UNPROCESSABLE_ENTITY
            )
        
        ## request can either be query for list of posts or for single post [id]
        if id == None:
            # check for posts with auto extend to locality on
            posts_extend = Post.objects.filter(
                extend_to_locality = True, locality__google_place_id = location['locality']
            )
            
            # check for posts without this but still within their privacy range 
            posts_no_extend = Post.objects.annotate(
                distance = haversine(F('coord_longitude'), F('coord_latitude'), location['coord_longitude'], location['coord_latitude'])
            ).filter(
                extend_to_locality = False, 
                locality__google_place_id = location['locality'], 
                distance__lte = F('range_meters')
            )
            combined_results = (posts_extend | posts_no_extend).distinct()
            
            # filter by post end_date
            results_active = (combined_results.filter(end_time__gt = datetime.now(timezone.utc)) | combined_results.filter(end_time = None)).distinct().order_by('-created_on') [page*limit:page*limit+limit]
            
            #create post serializer
            post_serializer = self.serializer_class(results_active, many = True)
            
            # human readability modifiers
            modified_data = add_ownership_from_id(post_serializer.data, request.user, many = True)
            modified_data = author_pk_to_username(modified_data, many = True)
            modified_data = place_id_to_name(modified_data, many = True)
            
            return Response({
                "status": "success",
                "data": modified_data
            }) 
        else:
            post = get_object_or_404(Post, pk = id)
            
            # ensure post is within users range
            in_range = (
                post.extend_to_locality and post.locality.google_place_id == location['locality']
            ) or (
                len(
                    Post.objects.filter(
                        pk = post.pk
                    ).annotate(
                        distance = haversine(F('coord_longitude'), F('coord_latitude'), location['coord_longitude'], location['coord_latitude'])
                    ).filter(
                        extend_to_locality = False, 
                        locality__google_place_id = location['locality'], 
                        distance__lte = F('range_meters')
                    )
                ) == 1
            )
            
            # if not in range or owned by requesting user, deny access
            if not user_is_user(request.user, post.author.username) and not in_range:
                return Response({
                    "status": "error",
                    "data": f"Post with id={id} outside of ranges (post/privacy range = {post.range_meters}, user/filter range = {location['range_meters']}).",
                    "code": "rangeError"
                }, status = HTTPStatus.UNAUTHORIZED)
                
            # create post serializer, annotating distance from user
            post_serializer = self.serializer_class(
                Post.objects.annotate(
                    distance = haversine(F('coord_longitude'), F('coord_latitude'), location['coord_longitude'], location['coord_latitude'])
                ).get(pk = post.id)
            )
            
            # human readability modifiers
            modified_data = add_ownership_from_id(post_serializer.data, request.user)
            modified_data = author_pk_to_username(modified_data)
            modified_data = place_id_to_name(modified_data)
        
            return Response({
                "status": "success",
                "data": modified_data
            })   
            
    def delete(self, request, id):
        post = get_object_or_404(Post, pk = id)
        if not user_is_user(request.user, post.author.username):
            return Response(status=status.HTTP_403_FORBIDDEN, data = {"detail": "Refused: cannot delete another users post :/."})
        if post.image != None:
            delete_image(post.image.delete_hash)
            post.image.delete()
        post.delete()
        return Response({
            "status": "success", "data": "Post deleted."
        })
    
    def patch(self, request, id=None):
        post = get_object_or_404(Post, pk = id)
        modified_request = request.data.copy()
        # remove these fields from request if present
        modified_request.pop('author', None)
        modified_request.pop('locality', None)
        modified_request.pop('created_on', None)
        modified_request.pop('coord_longitude', None)
        modified_request.pop('coord_latitude', None)
        modified_request = add_image(modified_request)
        if not user_is_user(request.user, post.author.username):
            return Response(status=status.HTTP_403_FORBIDDEN, data = {"detail": "Refused: cannot edit another users post >:|."})
        serializer = PostSerializer(post, modified_request, partial = True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        
        modified_data = place_id_to_name(serializer.data)
        return Response({"status": "success", "data": modified_request})
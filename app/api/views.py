from urllib import request, response
from rest_framework.views import APIView
from rest_framework import generics, permissions, mixins, authentication
from rest_framework.response import Response
from rest_framework import status
from dj_rest_auth.views import LogoutView

from django.contrib.auth.models import User
from api.models import Locality, Post

from api.request_modifiers import add_locality, add_ownership_from_id, author_pk_to_username, place_id_to_name
from .serializers import LocalitySerializer, PostSerializer, RegisterSerializer, UserSerializer
 
from .reverse_geocode import get_locality 
 
# Create your views here.
class TestAuthView(APIView):
    authentication_classes = (authentication.TokenAuthentication,)
    permission_classes = (permissions.IsAuthenticated,)
    
    def get(self, request):
        return Response(
            {"status" : "success", "data":"Hello {0}!".format(request.user)}
        )

class RegisterView(generics.GenericAPIView):
    serializer_class = RegisterSerializer
    def post(self, request):
        serializer = self.get_serializer(data = request.data)
        serializer.is_valid(raise_exception = True)     
        user = serializer.save()
        return Response(
            {
                "status": "success",
                "user": UserSerializer(user, context=self.get_serializer_context()).data
            }
        )

class TokenLogoutView(LogoutView):
    authentication_classes = (authentication.TokenAuthentication,)
    
class UserInfoView(APIView):
    authentication_classes = (authentication.TokenAuthentication,)
    permission_classes = (permissions.IsAuthenticated,)
    
    def get(self, request):
        lat = request.GET.get("lat")
        lon = request.GET.get("lon")
        
        locality = None
        if lat != None and lon != None:
            loc = add_locality({
                "coord_longitude" : lon,
                "coord_latitude" : lat
            })
            locality = LocalitySerializer(Locality.objects.get(google_place_id = loc['locality'])).data
        
        info = {
            "status": "success",
            "user": UserSerializer(request.user).data,
        }
        if locality != None:
            info.update({"locality" : locality})
        
        return Response(
            info
        )
        
class UserPosts(APIView):
    authentication_classes = (authentication.TokenAuthentication,)
    permission_classes = (permissions.IsAuthenticated,)
    
    def get(self, request):
        post_serializer = PostSerializer(
            Post.objects.filter(
                author = request.user
            ),
            many = True
        )
        modified_data = add_ownership_from_id(post_serializer.data, request.user, many = True)
        modified_data = author_pk_to_username(modified_data, many = True)
        modified_data = place_id_to_name(modified_data, many = True)
        return Response(
            {
                "posts" : modified_data
            }
        )
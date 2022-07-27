from http import HTTPStatus
from urllib import request, response
from django.forms import BooleanField
from django.shortcuts import get_object_or_404
from rest_framework.views import APIView
from rest_framework import generics, permissions, mixins, authentication
from rest_framework.response import Response
from rest_framework import status
from dj_rest_auth.views import LogoutView

from django.contrib.auth.models import User
from api.models import Locality, Post

from api.request_modifiers import add_locality
from .serializers import LocalitySerializer, PostSerializer, RegisterSerializer, UserSerializer
 
from .reverse_geocode import get_locality 

from api.request_modifiers import *
from api.distance import haversine
from django.db.models import F, Value
from django.db import models
 
# Create your views here.
class DebugMapPoints(APIView):
    # authentication_classes = (authentication.TokenAuthentication,)
    # permission_classes = (permissions.IsAuthenticated,)
    serializer_class = PostSerializer
    
    def get(self, request):
        location = {}
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
        if request.GET.get("range") != None:
            location.update({"range_meters": int(request.GET.get("range"))})
        if location["range_meters"] < 10 or location["range_meters"] > 10000:
            return Response (
                {
                    "data": "Filter range must be within range [10, 10000] (inclusive).",
                    "status": "error",
                    "code": "rangeRangeError"
                },
                status=HTTPStatus.BAD_REQUEST
            )
        modified_request = add_locality(location)
        
        if 'range_meters' in location:
            return self.get_by_coords(request, location)
        else:
            modified_request = add_locality(location)
                # return error is no_location locality (not enough info to continue query) 
            return self.get_by_locality(request, modified_request)

    def get_by_coords(self, request, location):
        posts = Post.objects.annotate(
                distance = haversine(F('coord_longitude'), F('coord_latitude'), location['coord_longitude'], location['coord_latitude'])
            ).filter(
                distance__lte=location['range_meters']
            ).filter(
                distance__lte=F('range_meters')   
            ).annotate(visible = Value(True, output_field = models.BooleanField())).order_by('-created_on')
            
        post_serializer = self.serializer_class((posts | Post.objects.all()).distinct(), many = True)
        modified_data = author_pk_to_username(post_serializer.data, many = True)
        modified_data = place_id_to_name(modified_data, many = True)
        
        return Response({
            "status": "success",
            "data": modified_data
        })
            
    def get_by_locality(self, request, location, id = None):
        if location['locality'] == "0":
            return Response(
                {
                    "status": "error",
                    "data": "By ommiting a range, locality mode was selected, but no valid locality could be found for the provided locality.",
                    "code" : "invalidLocalityError"
                },
                status=HTTPStatus.UNPROCESSABLE_ENTITY
            )
        
        posts_extend = Post.objects.filter(
            extend_to_locality = True, locality__google_place_id = location['locality']
        )
        posts_no_extend = Post.objects.annotate(
            distance = haversine(F('coord_longitude'), F('coord_latitude'), location['coord_longitude'], location['coord_latitude'])
        ).filter(
            extend_to_locality = False, 
            locality__google_place_id = location['locality'], 
            distance__lte = F('range_meters')
        )
        combined_results = (posts_extend | posts_no_extend).distinct().order_by('-created_on')
        
        post_serializer = self.serializer_class((combined_results | Post.objects.all()).distinct(), many = True)
        modified_data = author_pk_to_username(post_serializer.data, many = True)
        modified_data = place_id_to_name(modified_data, many = True)
        return Response({
            "status": "success",
            "data": modified_data
        }) 
    
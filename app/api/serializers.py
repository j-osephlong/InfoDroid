from dataclasses import field, fields
from rest_framework import serializers
from django.contrib.auth.models import User

from api.models import Locality, Post, Image
from django.contrib.auth.validators import UnicodeUsernameValidator

class RegisterSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ('id', 'username', 'password')
        extra_kwargs = {'password': {'write_only': True}}
        
    def create(self, validated_data):
        return User.objects.create_user(
            username = validated_data['username'], password = validated_data['password']
        )

class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = '__all__'
        
class LocalitySerializer(serializers.ModelSerializer):
    class Meta:
        model = Locality
        fields = '__all__'
        extra_kwargs = {
            'google_place_id': {'validators': [], 'read_only':True}
        }
        
class ImageSerializer(serializers.ModelSerializer):
    class Meta:
        model = Image
        fields = '__all__'
        
class PostSerializer(serializers.ModelSerializer):
    distance = serializers.FloatField(required = False)
    
    class Meta:
        model = Post
        fields = '__all__'
        
    def get_distance(self, obj):
        try:
            return obj.distance
        except:
            return None
        
    def get_visible(self, obj):
        try:
            return obj.visible
        except:
            return None
        
class CoordAndRangeSerializer(serializers.Serializer):
    coord_longitude = serializers.FloatField(required = False)
    coord_latitude = serializers.FloatField(required = False)
    range_meters = serializers.IntegerField(required = False)

    def validate(self, data):
        validated_data = data.copy()
        	
        coord_long = data.get('coord_longitude', None)
        coord_lat = data.get('coord_latitude', None)
        
        if coord_long == None or coord_lat == None:
                raise serializers.ValidationError("Must provide both coord_latitude and coord_longitude, and optionally range_meters.")

        return data
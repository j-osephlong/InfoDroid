import typing

from api.imgur_requests import upload_image
from .reverse_geocode import get_locality, no_location
from .models import Image, Locality
from .serializers import LocalitySerializer

from django.contrib.auth.models import User

from django.db.models.query import QuerySet

def add_locality(post_json: typing.Dict) -> typing.Dict:
    updated_post = post_json.copy()
    
    longitude = float(post_json['coord_longitude'])
    latitude = float(post_json['coord_latitude'])
    locality = get_locality(longitude, latitude)
    if len(Locality.objects.filter(google_place_id = locality['google_place_id'])) != 1:
        place = Locality(locality['name'], locality['google_place_id'])
        	
        place.save()
	
    updated_post.update({'locality': locality['google_place_id']})
    return updated_post

def add_image(post_json: typing.Dict) -> typing.Dict:
    updated_post = post_json.copy()
    
    image_data = updated_post.pop("image_base64", None)
    if image_data != None:
        print("Post contains image.")
        (success, imgur_response) = upload_image(image_data)
        if success:
            print("Post image uploaded:" + imgur_response['url'])
            image = Image(id = imgur_response['id'], url = imgur_response['url'], delete_hash = imgur_response['delete_hash'])
            image.save()
            updated_post.update({"image": image.pk})
    return updated_post

def add_ownership_from_id(post_json: typing.Dict, user: User, many: bool = False) -> typing.Dict:
    updated_post = post_json.copy()
    if not many:
        updated_post = [updated_post]
        
    for post in updated_post:
        isAuthor = post['author'] == user.id
        post.update({
            "is_my_post" : isAuthor
        })
        
    return updated_post if many else updated_post[0]

def place_id_to_name(post_json: typing.Dict, many: bool = False) -> typing.Dict:
    updated_post = post_json.copy()
    if not many:
        updated_post = [updated_post]
    	
    for post in updated_post:
        if 'locality' not in post:
            	
            post.update({"locality": no_location})
        elif len(Locality.objects.filter(google_place_id = post['locality'])) != 1:
            	
            post.update({"locality": no_location})
            	
        else:
            locality = Locality.objects.get(google_place_id = post['locality'])
            post.update({
                "locality" : {"name": locality.name, "google_place_id": locality.google_place_id}
            })
    return updated_post if many else updated_post[0]
    
def name_to_place_id(json: typing.Dict) -> typing.Dict:
    updated = json.copy()
    if 'locality' not in json:
        return updated
    if len(Locality.objects.filter(google_place_id = json['locality']['google_place_id'])) != 1:
        return updated
    locality = Locality.objects.get(google_place_id = json['locality']['google_place_id'])
    updated.update({
        "locality" : locality.google_place_id
    })
    
def author_pk_to_username(post_json: typing.Dict, many: bool = False):
    updated_post = post_json.copy()
    if not many:
        updated_post = [updated_post]
    for post in updated_post:
        if 'author' not in post:
            	
            return updated_post
        if len(User.objects.filter(pk = post['author'])) != 1:
            	
            return updated_post
        post.update({
            "author": User.objects.get(pk = post["author"]).username
        })
    return updated_post if many else updated_post[0]
        
def username_to_pk(post: typing.Dict):
    updated_post = post.copy()
    if 'author' not in post:
        return updated_post
    if not User.objects.exists(pk = post['author']):
        return updated_post
    updated_post.update({
        "author": User.objects.get(username = post["author"]).pk
    })
    return updated_post

def add_user_as_author(data: typing.Dict, user: User):
    updated = data.copy()
    updated.update({"author": user.id})
    return updated

def user_is_user(user: User, username: typing.AnyStr) -> bool:
    return user.username == username 
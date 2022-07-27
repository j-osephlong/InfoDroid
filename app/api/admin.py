from django.contrib import admin
from .models import Image, Locality, Post
# Register your models here.
admin.site.register(Locality)
admin.site.register(Post)
admin.site.register(Image)
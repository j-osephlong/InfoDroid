from datetime import datetime, timezone
from django.db import models
from django.contrib.auth.models import User
from django.core.validators import MinValueValidator, MaxValueValidator
import pytz
# Create your models here.

class Image(models.Model):
    id = models.CharField(max_length=10, primary_key=True, blank=False)
    url = models.URLField(blank=False, null=False)
    delete_hash = models.CharField(max_length=15, blank=False, null=False)
    
    def __str__(self) -> str:
        return f"{self.id}, stored at {self.url}"

class Locality(models.Model):
    name = models.CharField(blank=False, max_length=400)
    google_place_id = models.CharField(blank=False, primary_key=True, max_length=400)
    
    def __str__(self) -> str:
        return f"{self.name}, with Google place ID {self.google_place_id}"
    
class Post(models.Model):
    title = models.CharField(max_length=6000, blank=False)
    content = models.TextField(blank=False)
    ref_url = models.URLField(blank=True)
    author = models.ForeignKey(User, on_delete=models.CASCADE, blank=False)
    created_on = models.DateTimeField(auto_now_add=True, blank=False)
    coord_longitude = models.FloatField(blank = False, default = 0)
    coord_latitude = models.FloatField(blank = False, default = 0)
    range_meters = models.PositiveBigIntegerField(blank = False, default = 5000, validators = [MinValueValidator(10), MaxValueValidator(10000)])
    locality = models.ForeignKey(Locality, on_delete=models.CASCADE, blank=True, null=True)
    extend_to_locality = models.BooleanField(blank = False, default=False)
    image = models.ForeignKey(Image, on_delete=models.SET_NULL, blank=True, null=True)
    end_time = models.DateTimeField(blank = True, null = True)
    
    class Meta: 
        ordering = ["created_on"]
        get_latest_by = "created_on"

    def __str__(self):
        
        active = True
        print(self.end_time)
        if self.end_time is not None:
            if self.end_time < datetime.now(timezone.utc):
                active = False
            
        return "{0} {1} - {2} - {3} - active: {4}".format(self.id, self.title, self.author, self.created_on, active)
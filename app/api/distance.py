from math import atan2, sin, cos, sqrt, pi
from django.db.models import Func

# https://www.movable-type.co.uk/scripts/latlong.html
# formula source and implimentation notes
# consumes 4 floats as two coordiantes, outputs meters between them
# https://gist.github.com/rchrd2/5e0b014640a459a14ef038975d2a3683
# adapted to work for annotation of django QuerySets using link above
def haversine(long1: float, lat1: float, long2: float, lat2: float) -> float:
    #earth's radius in meters
    R = 6371e3
    lat1_radian = Radians(lat1)
    lat2_radian = Radians(lat2)
    long1_radian = Radians(long1)
    long2_radian = Radians(long2)
    
    a = Acos(Cos(lat1_radian) * Cos(lat2_radian) *
                Cos(long2_radian - long1_radian) +
                Sin(lat1_radian) * Sin(lat2_radian) 
            )
    
    return R * a

class Sin(Func):
    function = 'SIN'

class Cos(Func):
    function = 'COS'

class Acos(Func):
    function = 'ACOS'

class Radians(Func):
    function = 'RADIANS'
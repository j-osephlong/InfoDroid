import requests

api_key = 'KEY'
api_url = 'https://maps.googleapis.com/maps/api/geocode/json'

def get_locality(lng: float, lat: float) -> dict:
    query = {
        "result_type": "locality",
        "latlng": f"{lat},{lng}",
        "key": api_key, 
    }
    response = requests.get(api_url, params=query).json()
    if response['status'] != 'OK':
        return no_location
    try:
        return {
            "name": response['results'][0]['address_components'][0]['short_name'],
            "google_place_id": response['results'][0]['place_id']
        }
    except KeyError:
        	
        return no_location
    
no_location = {
    "name": "No proper location found.",
    "google_place_id": "0"
}
    
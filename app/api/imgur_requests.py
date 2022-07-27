import typing
import requests

client_id = 'CLIENT_ID'
api_url = 'https://api.imgur.com/3/image/'

def upload_image(base64: str) -> typing.Tuple[bool, dict]:
    data = {
        "image": base64
    }
    headers = {
        "Authorization" : f"Client-ID {client_id}"
    }
    response = requests.post(api_url, headers=headers, data=data).json()
    
    if response['status'] != 200:
        print(f"Failure to upload:\n___\n{str(response)}\n___")
        return (False, None)
    return (True, {
        "id": response['data']['id'],
        "url": response['data']['link'],
        "delete_hash": response['data']['deletehash']
    })
    
def delete_image(deletehash: str) -> bool:
    headers = {
        "Authorization" : f"Client-ID {client_id}"
    }
    response = requests.delete(api_url+deletehash, headers=headers).json()
    if response['status'] != 200:
        print(f"Failure to delete:\n___\n{str(response)}\n___")
        return False
    else:
        return True
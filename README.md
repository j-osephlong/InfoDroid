# InfoDroid
An app + REST server for written for my 2022 technical report class.

InfoDroid is a geolocked social media server and Android App. This means that users can make posts to their current local geographic area, and can only retrieve posts within the same area. The behaviour and design for this is better described in the file **technical-report.pdf** found in the root of this repository.

## Technical Report
Found in the root of this reporitory - **technical-report.pdf**

## Server 
All server files can be found in the **/app** directory. 

The server follows a REST API design, and uses the django web framework.

## Android App 
All android app files can be found in the **/appAndroid** directory.

The app uses Jetpack Compose for the user interface and Retrofit+GSON+OKHTTP for the backend.

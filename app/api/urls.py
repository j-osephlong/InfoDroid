from django.urls import path
from .views import TestAuthView, RegisterView, TokenLogoutView, UserInfoView, UserPosts
from dj_rest_auth.views import LoginView
from .post_views import PostView
from .debug_views import DebugMapPoints

urlpatterns = [
    path('test-auth/', TestAuthView.as_view()),
    path('login/', LoginView.as_view()),
    path('register/', RegisterView.as_view()),
    path('logout/', TokenLogoutView.as_view()),
    path('post/', PostView.as_view()),
    path('post/<int:id>/', PostView.as_view()),
    path('self/', UserInfoView.as_view()),
    path('self/posts/', UserPosts.as_view()),

    # path('debug/post/', DebugMapPoints.as_view())
]
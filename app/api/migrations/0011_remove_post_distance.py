# Generated by Django 4.0.1 on 2022-02-04 22:32

from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('api', '0010_post_distance'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='post',
            name='distance',
        ),
    ]
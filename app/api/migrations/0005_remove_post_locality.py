# Generated by Django 4.0.1 on 2022-01-20 15:51

from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('api', '0004_locality_post_locality'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='post',
            name='locality',
        ),
    ]

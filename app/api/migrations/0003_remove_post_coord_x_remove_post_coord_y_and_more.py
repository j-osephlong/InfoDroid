# Generated by Django 4.0.1 on 2022-01-19 22:12

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('api', '0002_post_coord_x_post_coord_y'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='post',
            name='coord_x',
        ),
        migrations.RemoveField(
            model_name='post',
            name='coord_y',
        ),
        migrations.AddField(
            model_name='post',
            name='coord_latitude',
            field=models.FloatField(default=0),
        ),
        migrations.AddField(
            model_name='post',
            name='coord_longitude',
            field=models.FloatField(default=0),
        ),
    ]

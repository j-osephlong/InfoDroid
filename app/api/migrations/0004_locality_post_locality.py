# Generated by Django 4.0.1 on 2022-01-20 13:59

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('api', '0003_remove_post_coord_x_remove_post_coord_y_and_more'),
    ]

    operations = [
        migrations.CreateModel(
            name='Locality',
            fields=[
                ('name', models.CharField(max_length=400)),
                ('google_place_id', models.CharField(max_length=400, primary_key=True, serialize=False)),
            ],
        ),
        migrations.AddField(
            model_name='post',
            name='locality',
            field=models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.CASCADE, to='api.locality'),
        ),
    ]

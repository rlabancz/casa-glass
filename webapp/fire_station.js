var converter = require('coordinator');
var geolib = require('geolib');
var csv = require('csv');

fn = converter('utm', 'latlong');



var g_data;

csv().from.path(__dirname+'/data/fire_stations.csv').to.array(function(data) {
    g_data = data;
});

exports.closest = function(query) {

    var closest_distance, closest_station; 


    g_data.forEach(function(station) {

        var latlong = fn(station[2], station[1], 17);
        //console.log(latlong);

        var here = {latitude:'43.660303', longitude:'-79.388653'};

        var distance = geolib.getDistance({latitude:query.latitude, longitude:query.longitude}, latlong);


        if(closest_station===undefined || distance < closest_station.distance) {
            console.log('closest station');
            console.log(distance);
            closest_station = latlong;
            closest_station.distance = distance;
        } 
        
    });

    debugger;

    return {
        latitude:closest_station.latitude,
        longitude:closest_station.longitude,
        distance:closest_station.distance
    }


}

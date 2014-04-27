var geolib = require('geolib'),
    fs = require('fs');

exports.closest = closest;

data = JSON.parse(fs.readFileSync(__dirname+'/data/bike_stations.json'));

function closest(query) {

    var closest_station;

    data.stationBeanList.forEach(function(station) {
        var distance = geolib.getDistance(query, station);
        if(closest_station===undefined || distance < closest_station.distance) {
            closest_station = station;
            closest_station.distance = distance;
        }

    });

    return closest_station;


}

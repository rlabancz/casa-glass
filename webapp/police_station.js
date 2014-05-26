var shapefile = require('shapefile'),
    geolib = require('geolib');

var g_data;

shapefile.read(__dirname+'/data/police_facilities/Toronto_Police_Facilities_WGS84.shp', function(err, data) {
    g_data = data;
});

exports.closest = function(query) {
    var closest_station;
    g_data.features.forEach(function(station) {
        var p = station.geometry.coordinates;
        console.log(p);
        var distance = geolib.getDistance(query, {latitude:p[1], longitude:p[0]});
        if(closest_station===undefined || distance < closest_station.distance) {
            closest_station = station.properties;
            closest_station.distance = distance;
        }
    });

    return closest_station;
}

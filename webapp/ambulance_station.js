var shapefile = require('shapefile'),
    geolib = require('geolib');

var g_data;

shapefile.read(__dirname+'/data/ambulance_facility/AMBULANCE_FACILITY_WGS84.shp', function(err, data) {
    g_data = data;
});


exports.closest = function(query) {
    var closest_station;
    g_data.features.forEach(function(station) {
        var p = station.properties;
        var distance = geolib.getDistance(query, {latitude:p['LATITUDE'], longitude:p['LONGITUDE']});
        if(closest_station===undefined || distance < closest_station.distance) {
            closest_station = p;
            closest_station.distance = distance;
        }
    });

    return closest_station;
}

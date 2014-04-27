var shapefile = require('shapefile'),
    geolib = require('geolib');

var g_data;

shapefile.read(__dirname+'/data/school_tdsb/SCHOOL_TDSB_UTM.shp', function(err, data) {
    g_data = data;
});

exports.closest = function(query) {
    var closest_school;
    g_data.features.forEach(function(school) {
        var p = school.properties;
        var distance = geolib.getDistance(query, {latitude:p['LATITUDE'], longitude:p['LONGITUDE']});

        if(closest_school===undefined || distance < closest_school.distance) {
            closest_school = school.properties;
            closest_school.distance = distance;
        }

    });

    return closest_school;
}

var urlParams = new URLSearchParams(window.location.search);
var lat = urlParams.get('lat') ?? -7.7496395;
var lng = urlParams.get('lng') ?? 113.426669;
var zoom = urlParams.get('zoom') ?? 12;
var provider = urlParams.get('provider') ?? 'OpenStreetMap';
var radius = urlParams.get('radius') ?? 300;
var cLat = urlParams.get('cLat') ?? lat;
var cLng = urlParams.get('cLng') ?? lng;

var currentLayer = L.tileLayer.provider(provider, {
    className: 'map-tiles',
    referrerPolicy: 'strict-origin-when-cross-origin',
});

var map = L.map('map', {
    zoomControl: false,
    layers: [currentLayer]
}).setView([lat, lng], zoom);

L.control.zoom({position: 'bottomright'}).addTo(map);

var circle = L.circle([cLat, cLng], {
    color: 'red',
    fillColor: '#f03',
    fillOpacity: 0.2,
    radius: radius
}).addTo(map);

function changeLayer(providerName) {
    map.removeLayer(currentLayer);
    currentLayer = L.tileLayer.provider(providerName, {
        className: providerName === 'Esri.WorldImagery' ? '' : 'map-tiles',
        referrerPolicy: 'strict-origin-when-cross-origin'
    }).addTo(map);
}

var icon = L.icon({
    iconUrl: 'marker-icon-2x.png',
    shadowUrl: 'marker-shadow.png',
    iconSize:     [27, 44],
    iconAnchor:   [14, 48],
    shadowSize:   [50, 64],
    shadowAnchor: [17, 68 ],
    popupAnchor:  [0, 0]
});

var mapMarker;
var searchInput = document.getElementById("address_input");

async function searchForAddress() {
    var coords = await searchAddress(searchInput.value);
    if (coords === undefined || coords.length < 2) return;
    var lat = coords[0];
    var lng = coords[1];
    var mapEvent = { latlng: L.latLng(lat, lng) };
    setOnMap(lat, lng);
    onMapClick(mapEvent);
}

function onMarkerDragEnd(e) {
    var latlng = e.target.getLatLng().wrap();
    var wrap = "LatLng(" + latlng.lat + ", " + latlng.lng + ")";
    Android.setPosition(wrap);
}

function onMapClick(e) {
    if (typeof mapMarker != 'undefined')
        map.removeLayer(mapMarker);
    mapMarker = L.marker(e.latlng, {icon: icon, draggable: true}).addTo(map);
    mapMarker.on('dragend', onMarkerDragEnd);
    var latlng = e.latlng.wrap();
    var wrap = "LatLng(" + latlng.lat + ", " + latlng.lng + ")";
    Android.setPosition(wrap);
}

function onZoomEnd(e) {
    Android.setZoom(map.getZoom());
}

function updateCircle(aLat, aLng, aRadius) {
    if (aRadius !== undefined) circle.setRadius(aRadius);
    circle.setLatLng([aLat, aLng]);
}

function setOnMap(aLat, aLng, aRadius) {
    if (typeof mapMarker != 'undefined')
        map.removeLayer(mapMarker);
    zoom = map.getZoom();
    map.setView(new L.LatLng(aLat, aLng), zoom);
    mapMarker = L.marker([aLat, aLng], {icon: icon, draggable: true}).addTo(map);
    mapMarker.on('dragend', onMarkerDragEnd);
}

map.on('click', onMapClick);
map.on('zoomend', onZoomEnd);

mapMarker = L.marker([lat, lng], {
    icon: icon,
    draggable: true
}).addTo(map);
mapMarker.on('dragend', onMarkerDragEnd);

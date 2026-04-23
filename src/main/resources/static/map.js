var map = L.map('map').setView([37.9838, 23.7275], 13);

L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors'
}).addTo(map);

var listEl = document.getElementById('locations-list');
var filterInput = document.getElementById('location-filter');
var categoryFilter = document.getElementById('category-filter');
var ratingFilter = document.getElementById('rating-filter');
var favoritesOnlyCheckbox = document.getElementById('favorites-only');
var withNotesOnlyCheckbox = document.getElementById('with-notes-only');
var nearMeOnlyCheckbox = document.getElementById('near-me-only');
var useMyLocationBtn = document.getElementById('use-my-location');
var clearFiltersBtn = document.getElementById('clear-filters');
var exportJsonBtn = document.getElementById('export-json');
var importJsonInput = document.getElementById('import-json');
var drawRouteBtn = document.getElementById('draw-route');
var clearRouteBtn = document.getElementById('clear-route');
var routeInfoEl = document.getElementById('route-info');

var drawerEl = document.getElementById('edit-drawer');
var drawerCloseBtn = document.getElementById('drawer-close');
var drawerMetaEl = document.getElementById('drawer-meta');
var drawerNameEl = document.getElementById('drawer-name');
var drawerNotesEl = document.getElementById('drawer-notes');
var drawerCategoryEl = document.getElementById('drawer-category');
var drawerFavoriteEl = document.getElementById('drawer-favorite');
var drawerVisitedEl = document.getElementById('drawer-visited');
var drawerRatingEl = document.getElementById('drawer-rating');
var drawerPhotosEl = document.getElementById('drawer-photos');
var drawerThumbsEl = document.getElementById('drawer-thumbs');
var drawerSaveBtn = document.getElementById('drawer-save');
var drawerDeleteBtn = document.getElementById('drawer-delete');

var allLocations = [];
var markersById = {};
var selectedLocationId = null;
var userLatLng = null;
var routeSelectedIds = [];
var routeLine = null;

function haversineKm(a, b) {
    var R = 6371;
    var dLat = (b.lat - a.lat) * Math.PI / 180;
    var dLng = (b.lng - a.lng) * Math.PI / 180;
    var lat1 = a.lat * Math.PI / 180;
    var lat2 = b.lat * Math.PI / 180;
    var x = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.sin(dLng / 2) * Math.sin(dLng / 2) * Math.cos(lat1) * Math.cos(lat2);
    var c = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
    return R * c;
}

function categoryEmoji(cat) {
    switch ((cat || 'OTHER').toUpperCase()) {
        case 'PARK': return '🌳';
        case 'VET': return '🩺';
        case 'GROOMING': return '✂️';
        case 'CAFE': return '☕';
        case 'WATER': return '💧';
        case 'BAGS': return '🧻';
        default: return '📍';
    }
}

function paws(rating) {
    var r = parseInt(rating, 10);
    if (!r || r < 1) return '';
    return ' ' + '🐾'.repeat(Math.min(5, r));
}

function markerForLocation(loc, popupHtml) {
    var emoji = categoryEmoji(loc.category);
    var fav = loc.favorite ? '⭐' : '';
    var icon = L.divIcon({
        className: 'dog-spot-icon',
        html: '<div style="width:28px;height:28px;border-radius:999px;display:flex;align-items:center;justify-content:center;' +
            'background:linear-gradient(135deg, rgba(245,158,11,0.92), rgba(96,165,80,0.92));' +
            'border:1px solid rgba(15,23,42,0.65);box-shadow:0 10px 22px rgba(0,0,0,0.45);font-size:14px;">' +
            emoji + fav + '</div>',
        iconSize: [28, 28],
        iconAnchor: [14, 28],
        popupAnchor: [0, -28]
    });
    return L.marker([loc.lat, loc.lng], {icon: icon}).addTo(map).bindPopup(popupHtml);
}

function formatCoord(value) {
    return value.toFixed(5);
}

function clearList() {
    while (listEl.firstChild) {
        listEl.removeChild(listEl.firstChild);
    }
}

function renderEmptyState() {
    var p = document.createElement('p');
    p.className = 'empty-state';
    p.textContent = 'No locations yet. Click on the map to add one.';
    listEl.appendChild(p);
}

function addLocationRow(loc) {
    var row = document.createElement('div');
    row.className = 'location-row';

    var nameEl = document.createElement('div');
    nameEl.className = 'location-name';
    nameEl.textContent = (categoryEmoji(loc.category) + ' ' + (loc.name || 'Unnamed location') + paws(loc.rating));

    if (loc.notes && loc.notes.trim() !== '') {
        var notesEl = document.createElement('div');
        notesEl.className = 'location-notes';
        notesEl.textContent = loc.notes;
        row.appendChild(notesEl);
    }

    var coordsEl = document.createElement('div');
    coordsEl.className = 'location-coords';
    coordsEl.textContent =
        'Lat: ' + formatCoord(loc.lat) + '  |  Lng: ' + formatCoord(loc.lng);

    // layout: name/notes on left, delete on right
    var left = document.createElement('div');
    left.style.flex = '1';
    left.appendChild(nameEl);
    left.appendChild(coordsEl);

    var routePick = document.createElement('label');
    routePick.style.display = 'inline-flex';
    routePick.style.alignItems = 'center';
    routePick.style.gap = '6px';
    routePick.style.marginRight = '8px';
    routePick.style.fontSize = '11px';
    routePick.style.color = '#e5e7eb';

    var routeCb = document.createElement('input');
    routeCb.type = 'checkbox';
    routeCb.checked = routeSelectedIds.indexOf(loc.id) !== -1;
    routeCb.addEventListener('click', function (e) {
        e.stopPropagation();
    });
    routeCb.addEventListener('change', function () {
        if (typeof loc.id === 'undefined' || loc.id === null) {
            return;
        }
        var idx = routeSelectedIds.indexOf(loc.id);
        if (routeCb.checked && idx === -1) {
            routeSelectedIds.push(loc.id);
        } else if (!routeCb.checked && idx !== -1) {
            routeSelectedIds.splice(idx, 1);
        }
        updateRouteInfo();
    });

    routePick.appendChild(routeCb);
    routePick.appendChild(document.createTextNode('Route'));

    var actions = document.createElement('button');
    actions.textContent = 'Delete';
    actions.style.marginLeft = '8px';
    actions.style.padding = '2px 8px';
    actions.style.fontSize = '11px';
    actions.style.borderRadius = '999px';
    actions.style.border = '1px solid rgba(148,163,184,0.6)';
    actions.style.background = 'transparent';
    actions.style.color = '#f97373';
    actions.style.cursor = 'pointer';

    var container = document.createElement('div');
    container.style.display = 'flex';
    container.style.alignItems = 'center';
    container.appendChild(routePick);
    container.appendChild(left);
    container.appendChild(actions);

    row.appendChild(container);

    // focus + open editor drawer on click (left side)
    left.style.cursor = 'pointer';
    left.title = 'Click to focus & edit this location';
    left.addEventListener('click', function () {
        if (typeof loc.id === 'undefined' || loc.id === null) {
            return;
        }

        var marker = markersById[loc.id];
        if (marker) {
            map.setView(marker.getLatLng(), map.getZoom());
            marker.openPopup();
        }
        openDrawerForLocation(loc.id);
    });

    // delete button
    actions.addEventListener('click', function (event) {
        event.stopPropagation();
        if (typeof loc.id === 'undefined' || loc.id === null) {
            return;
        }
        var confirmDelete = confirm('Delete "' + (loc.name || 'this location') + '"?');
        if (!confirmDelete) {
            return;
        }

        fetch('/api/locations/' + loc.id, {
            method: 'DELETE'
        })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('Failed to delete location');
                }
                window.location.reload();
            })
            .catch(function () {
                alert('Could not delete location. Please try again.');
            });
    });

    listEl.appendChild(row);
}

function renderList() {
    clearList();

    if (!Array.isArray(allLocations) || allLocations.length === 0) {
        renderEmptyState();
        return;
    }

    var term = (filterInput && filterInput.value ? filterInput.value.toLowerCase() : '').trim();
    var notesOnly = !!(withNotesOnlyCheckbox && withNotesOnlyCheckbox.checked);
    var favoritesOnly = !!(favoritesOnlyCheckbox && favoritesOnlyCheckbox.checked);
    var nearMeOnly = !!(nearMeOnlyCheckbox && nearMeOnlyCheckbox.checked);
    var category = (categoryFilter && categoryFilter.value ? categoryFilter.value : '').trim();
    var minRating = (ratingFilter && ratingFilter.value ? parseInt(ratingFilter.value, 10) : null);

    var filtered = allLocations.filter(function (loc) {
        if (notesOnly && (!loc.notes || loc.notes.trim() === '')) {
            return false;
        }
        if (favoritesOnly && !loc.favorite) {
            return false;
        }
        if (category && ((loc.category || 'OTHER').toUpperCase() !== category.toUpperCase())) {
            return false;
        }
        if (minRating && (!loc.rating || parseInt(loc.rating, 10) < minRating)) {
            return false;
        }
        if (nearMeOnly && (!userLatLng || typeof loc.lat === 'undefined')) {
            return false;
        }
        if (!term) {
            return true;
        }
        var name = (loc.name || '').toLowerCase();
        var notes = (loc.notes || '').toLowerCase();
        return name.indexOf(term) !== -1 || notes.indexOf(term) !== -1;
    });

    if (userLatLng) {
        filtered.sort(function (a, b) {
            return haversineKm(userLatLng, a) - haversineKm(userLatLng, b);
        });
    }

    if (filtered.length === 0) {
        renderEmptyState();
        return;
    }

    filtered.forEach(function (loc) {
        addLocationRow(loc);
    });
}

fetch('/api/locations')
    .then(function (response) {
        return response.json();
    })
    .then(function (data) {
        if (!Array.isArray(data)) {
            data = [];
        }

        allLocations = data;

        allLocations.forEach(function (loc) {
            var popupText = '<strong>' + (loc.name || 'Unnamed location') + '</strong>' + paws(loc.rating);
            if (loc.notes && loc.notes.trim() !== '') {
                popupText += '<br/><span style="font-size: 12px; color: #6b7280;">' +
                    loc.notes +
                    '</span>';
            }
            if (loc.photos && loc.photos.length > 0) {
                popupText += '<br/><img src="' + loc.photos[0] + '" style="margin-top:6px; width: 140px; height: 90px; object-fit: cover; border-radius: 10px; border: 1px solid rgba(148,163,184,0.35);" />';
            }
            var marker = markerForLocation(loc, popupText);
            if (typeof loc.id !== 'undefined' && loc.id !== null) {
                markersById[loc.id] = marker;
            }
        });

        renderList();
    })
    .catch(function () {
        // if the initial load fails, still show an empty state
        clearList();
        renderEmptyState();
    });

map.on('click', function (e) {
    var lat = e.latlng.lat;
    var lng = e.latlng.lng;

    var name = prompt('Give this place a short name:', 'New dog spot');
    if (name === null || name.trim() === '') {
        return;
    }

    var notes = prompt('Optional: add a short note or description:', '');

    var newLocation = {
        lat: lat,
        lng: lng,
        name: name.trim(),
        notes: notes && notes.trim() !== '' ? notes.trim() : null,
        category: 'OTHER',
        visited: false,
        favorite: false,
        rating: null,
        photos: []
    };

    fetch('/api/locations', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(newLocation)
    })
        .then(function (response) {
            if (!response.ok) {
                throw new Error('Failed to save location');
            }
            return response.json();
        })
        .then(function (saved) {
            var popupText = '<strong>' + (saved.name || 'Unnamed location') + '</strong>' + paws(saved.rating);
            if (saved.notes && saved.notes.trim() !== '') {
                popupText += '<br/><span style="font-size: 12px; color: #6b7280;">' +
                    saved.notes +
                    '</span>';
            }
            var marker = markerForLocation(saved, popupText);
            if (typeof saved.id !== 'undefined' && saved.id !== null) {
                markersById[saved.id] = marker;
            }
            marker.openPopup();

            allLocations.push(saved);
            renderList();
        })
        .catch(function () {
            alert('Could not save location. Please try again.');
        });
});

if (filterInput) {
    filterInput.addEventListener('input', function () {
        renderList();
    });
}

if (withNotesOnlyCheckbox) {
    withNotesOnlyCheckbox.addEventListener('change', function () {
        renderList();
    });
}

function openDrawerForLocation(id) {
    var loc = allLocations.find(function (l) { return l.id === id; });
    if (!loc || !drawerEl) {
        return;
    }
    selectedLocationId = id;
    drawerEl.classList.add('open');
    drawerEl.setAttribute('aria-hidden', 'false');

    drawerMetaEl.textContent = categoryEmoji(loc.category) + ' #' + loc.id +
        ' • ' + formatCoord(loc.lat) + ', ' + formatCoord(loc.lng);
    drawerNameEl.value = loc.name || '';
    drawerNotesEl.value = loc.notes || '';
    drawerCategoryEl.value = (loc.category || 'OTHER').toUpperCase();
    drawerFavoriteEl.checked = !!loc.favorite;
    drawerVisitedEl.checked = !!loc.visited;
    drawerRatingEl.value = loc.rating ? String(loc.rating) : '';

    renderDrawerThumbs(loc.photos || []);
}

function closeDrawer() {
    if (!drawerEl) return;
    drawerEl.classList.remove('open');
    drawerEl.setAttribute('aria-hidden', 'true');
}

function renderDrawerThumbs(photos) {
    if (!drawerThumbsEl) return;
    while (drawerThumbsEl.firstChild) drawerThumbsEl.removeChild(drawerThumbsEl.firstChild);
    (photos || []).slice(0, 9).forEach(function (p) {
        var img = document.createElement('img');
        img.src = p;
        drawerThumbsEl.appendChild(img);
    });
}

function updateRouteInfo() {
    if (!routeInfoEl) return;
    if (!routeSelectedIds || routeSelectedIds.length < 2) {
        routeInfoEl.textContent = 'No route selected.';
        return;
    }
    routeInfoEl.textContent = routeSelectedIds.length + ' stops selected.';
}

function redrawRoute() {
    if (routeLine) {
        map.removeLayer(routeLine);
        routeLine = null;
    }
    if (!routeSelectedIds || routeSelectedIds.length < 2) {
        updateRouteInfo();
        return;
    }
    var points = routeSelectedIds
        .map(function (id) { return allLocations.find(function (l) { return l.id === id; }); })
        .filter(Boolean)
        .map(function (l) { return [l.lat, l.lng]; });
    if (points.length < 2) {
        updateRouteInfo();
        return;
    }
    routeLine = L.polyline(points, {color: '#f59e0b', weight: 4, opacity: 0.85}).addTo(map);
    map.fitBounds(routeLine.getBounds(), {padding: [18, 18]});

    var km = 0;
    for (var i = 1; i < points.length; i++) {
        km += haversineKm({lat: points[i - 1][0], lng: points[i - 1][1]}, {lat: points[i][0], lng: points[i][1]});
    }
    routeInfoEl.textContent = points.length + ' stops • ~' + km.toFixed(2) + ' km';
}

if (drawerCloseBtn) drawerCloseBtn.addEventListener('click', closeDrawer);

if (drawerSaveBtn) {
    drawerSaveBtn.addEventListener('click', function () {
        if (selectedLocationId === null) return;
        var loc = allLocations.find(function (l) { return l.id === selectedLocationId; });
        if (!loc) return;

        var updated = {
            id: loc.id,
            lat: loc.lat,
            lng: loc.lng,
            name: (drawerNameEl.value || '').trim(),
            notes: (drawerNotesEl.value || '').trim() || null,
            category: drawerCategoryEl.value,
            favorite: !!drawerFavoriteEl.checked,
            visited: !!drawerVisitedEl.checked,
            rating: drawerRatingEl.value ? parseInt(drawerRatingEl.value, 10) : null,
            photos: loc.photos || []
        };

        fetch('/api/locations/' + loc.id, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(updated)
        })
            .then(function (r) {
                if (!r.ok) throw new Error('save failed');
                return r.json();
            })
            .then(function (saved) {
                // update in-memory list
                var idx = allLocations.findIndex(function (l) { return l.id === saved.id; });
                if (idx !== -1) allLocations[idx] = saved;

                // recreate marker to update icon/popup
                var oldMarker = markersById[saved.id];
                if (oldMarker) map.removeLayer(oldMarker);
                var popupText = '<strong>' + (saved.name || 'Unnamed location') + '</strong>' + paws(saved.rating);
                if (saved.notes && saved.notes.trim() !== '') {
                    popupText += '<br/><span style="font-size: 12px; color: #6b7280;">' + saved.notes + '</span>';
                }
                if (saved.photos && saved.photos.length > 0) {
                    popupText += '<br/><img src="' + saved.photos[0] + '" style="margin-top:6px; width: 140px; height: 90px; object-fit: cover; border-radius: 10px; border: 1px solid rgba(148,163,184,0.35);" />';
                }
                markersById[saved.id] = markerForLocation(saved, popupText);

                renderList();
                openDrawerForLocation(saved.id);
            })
            .catch(function () {
                alert('Could not save changes.');
            });
    });
}

if (drawerDeleteBtn) {
    drawerDeleteBtn.addEventListener('click', function () {
        if (selectedLocationId === null) return;
        var loc = allLocations.find(function (l) { return l.id === selectedLocationId; });
        if (!loc) return;
        if (!confirm('Delete "' + (loc.name || 'this location') + '"?')) return;
        fetch('/api/locations/' + loc.id, {method: 'DELETE'})
            .then(function (r) {
                if (!r.ok) throw new Error('delete failed');
                // update in-memory
                allLocations = allLocations.filter(function (l) { return l.id !== loc.id; });
                if (markersById[loc.id]) {
                    map.removeLayer(markersById[loc.id]);
                    delete markersById[loc.id];
                }
                closeDrawer();
                renderList();
            })
            .catch(function () {
                alert('Could not delete.');
            });
    });
}

if (drawerPhotosEl) {
    drawerPhotosEl.addEventListener('change', function () {
        if (selectedLocationId === null) return;
        var loc = allLocations.find(function (l) { return l.id === selectedLocationId; });
        if (!loc) return;
        var files = Array.prototype.slice.call(drawerPhotosEl.files || []);
        if (files.length === 0) return;

        var readers = files.slice(0, 6).map(function (file) {
            return new Promise(function (resolve) {
                var fr = new FileReader();
                fr.onload = function () { resolve(fr.result); };
                fr.onerror = function () { resolve(null); };
                fr.readAsDataURL(file);
            });
        });

        Promise.all(readers).then(function (results) {
            var imgs = results.filter(Boolean);
            loc.photos = (loc.photos || []).concat(imgs).slice(0, 9);
            renderDrawerThumbs(loc.photos);
        });
    });
}

function clearFilters() {
    if (filterInput) filterInput.value = '';
    if (categoryFilter) categoryFilter.value = '';
    if (ratingFilter) ratingFilter.value = '';
    if (favoritesOnlyCheckbox) favoritesOnlyCheckbox.checked = false;
    if (withNotesOnlyCheckbox) withNotesOnlyCheckbox.checked = false;
    if (nearMeOnlyCheckbox) nearMeOnlyCheckbox.checked = false;
    renderList();
}

if (clearFiltersBtn) clearFiltersBtn.addEventListener('click', clearFilters);

function bindFilter(el, evt) {
    if (!el) return;
    el.addEventListener(evt, function () { renderList(); });
}
bindFilter(categoryFilter, 'change');
bindFilter(ratingFilter, 'change');
bindFilter(favoritesOnlyCheckbox, 'change');
bindFilter(nearMeOnlyCheckbox, 'change');

if (useMyLocationBtn) {
    useMyLocationBtn.addEventListener('click', function () {
        if (!navigator.geolocation) {
            alert('Geolocation not supported in this browser.');
            return;
        }
        navigator.geolocation.getCurrentPosition(function (pos) {
            userLatLng = {lat: pos.coords.latitude, lng: pos.coords.longitude};
            map.setView([userLatLng.lat, userLatLng.lng], Math.max(map.getZoom(), 14));
            renderList();
        }, function () {
            alert('Could not get your location. Please allow location access.');
        }, {enableHighAccuracy: true, timeout: 8000});
    });
}

if (drawRouteBtn) drawRouteBtn.addEventListener('click', redrawRoute);
if (clearRouteBtn) {
    clearRouteBtn.addEventListener('click', function () {
        routeSelectedIds = [];
        if (routeLine) {
            map.removeLayer(routeLine);
            routeLine = null;
        }
        updateRouteInfo();
        renderList();
    });
}

if (exportJsonBtn) {
    exportJsonBtn.addEventListener('click', function () {
        var payload = JSON.stringify(allLocations, null, 2);
        var blob = new Blob([payload], {type: 'application/json'});
        var url = URL.createObjectURL(blob);
        var a = document.createElement('a');
        a.href = url;
        a.download = 'dog-spots.json';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    });
}

if (importJsonInput) {
    importJsonInput.addEventListener('change', function () {
        var file = importJsonInput.files && importJsonInput.files[0];
        if (!file) return;
        var fr = new FileReader();
        fr.onload = function () {
            try {
                var parsed = JSON.parse(fr.result);
                if (!Array.isArray(parsed)) throw new Error('not array');
                fetch('/api/locations/import', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(parsed)
                })
                    .then(function (r) {
                        if (!r.ok) throw new Error('import failed');
                        window.location.reload();
                    })
                    .catch(function () {
                        alert('Import failed.');
                    });
            } catch (e) {
                alert('Invalid JSON file.');
            }
        };
        fr.readAsText(file);
    });
}

updateRouteInfo();
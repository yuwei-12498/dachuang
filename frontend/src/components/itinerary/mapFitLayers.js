export function collectLeafletBoundsLayers(layers = []) {
  const boundsLayers = []

  const visit = layer => {
    if (!layer) {
      return
    }

    if (typeof layer.getLayers === 'function') {
      const nestedLayers = layer.getLayers()
      if (Array.isArray(nestedLayers)) {
        nestedLayers.forEach(visit)
      }
      return
    }

    if (typeof layer.eachLayer === 'function') {
      layer.eachLayer(visit)
      return
    }

    if (typeof layer.getBounds === 'function' || typeof layer.getLatLng === 'function') {
      boundsLayers.push(layer)
    }
  }

  layers.forEach(visit)
  return boundsLayers
}

export function buildLeafletFitTarget(L, layers = []) {
  const boundsLayers = collectLeafletBoundsLayers(layers)
  if (!boundsLayers.length) {
    return null
  }
  return L.featureGroup(boundsLayers)
}

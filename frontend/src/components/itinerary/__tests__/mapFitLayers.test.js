import { describe, expect, it, vi } from 'vitest'
import { buildLeafletFitTarget, collectLeafletBoundsLayers } from '../mapFitLayers'

describe('mapFitLayers', () => {
  it('flattens nested layer groups before passing layers into Leaflet bounds fitting', () => {
    const polyline = { getBounds: () => ({ isValid: () => true }) }
    const markerA = { getLatLng: () => [30.1, 104.1] }
    const markerB = { getLatLng: () => [30.2, 104.2] }
    const nestedLayerGroup = {
      getLayers: () => [markerA, markerB]
    }
    const unsupportedLayer = { remove: () => {} }

    expect(
      collectLeafletBoundsLayers([polyline, nestedLayerGroup, unsupportedLayer])
    ).toEqual([polyline, markerA, markerB])
  })

  it('builds a feature group only from bounds-capable layers', () => {
    const marker = { getLatLng: () => [30.6, 104.0] }
    const polyline = { getBounds: () => ({ isValid: () => true }) }
    const featureGroup = { getBounds: () => ({ isValid: () => true }) }
    const L = {
      featureGroup: vi.fn(() => featureGroup)
    }

    const fitTarget = buildLeafletFitTarget(L, [{ getLayers: () => [marker] }, polyline, null, { foo: 'bar' }])

    expect(L.featureGroup).toHaveBeenCalledWith([marker, polyline])
    expect(fitTarget).toBe(featureGroup)
  })

  it('returns null when there is no valid layer to fit', () => {
    const L = {
      featureGroup: vi.fn()
    }

    expect(buildLeafletFitTarget(L, [null, { foo: 'bar' }])).toBeNull()
    expect(L.featureGroup).not.toHaveBeenCalled()
  })
})

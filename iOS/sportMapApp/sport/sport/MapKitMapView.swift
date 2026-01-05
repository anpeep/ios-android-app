//
//  MapKitMapView.swift
//  sport
//
//  Created by Anne-Riin Peep on 05.01.2026.
//


import MapKit
import SwiftUI

struct MapKitMapView: UIViewRepresentable {
    @ObservedObject var locationManager: LocationManager

    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView()
        map.delegate = context.coordinator
        map.showsUserLocation = true
        map.userTrackingMode = .none
        map.isRotateEnabled = true
        map.isPitchEnabled = false
        return map
    }

    func updateUIView(_ map: MKMapView, context: Context) {
        map.removeOverlays(map.overlays)

        let coords = locationManager.trail.map { $0.coordinate }
        guard coords.count > 1 else { return }

        let polyline = MKPolyline(coordinates: coords, count: coords.count)
        map.addOverlay(polyline)

        if locationManager.keepCentered, let last = coords.last {
            map.setCenter(last, animated: true)
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    final class Coordinator: NSObject, MKMapViewDelegate {
        func mapView(
            _ mapView: MKMapView,
            rendererFor overlay: MKOverlay
        ) -> MKOverlayRenderer {

            if let polyline = overlay as? MKPolyline {
                let r = MKPolylineRenderer(polyline: polyline)
                r.lineWidth = 4
                r.strokeColor = .systemBlue
                return r
            }
            return MKOverlayRenderer()
        }
    }
}

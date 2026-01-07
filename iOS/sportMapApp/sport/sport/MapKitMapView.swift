//
//  MapKitMapView.swift
//  sport
//
//  Created by Anne-Riin Peep on 05.01.2026.
//
import CoreLocation



import MapKit
import SwiftUI

struct MapKitMapView: UIViewRepresentable {
    // 1. Ensure this is exactly as written
    @ObservedObject var locationManager: LocationManager
    
    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView()
        map.delegate = context.coordinator
        map.showsUserLocation = true
        return map
    }

    func updateUIView(_ map: MKMapView, context: Context) {
        // Clear and redraw path
        map.removeOverlays(map.overlays)
        let coords = locationManager.route
        
        if coords.count >= 2 {
            let polyline = MKPolyline(coordinates: coords, count: coords.count)
            map.addOverlay(polyline)
        }
        
        // 2. Access property directly. If error persists, clean the build (Cmd+Shift+K)
        if locationManager.keepCentered, let last = coords.last {
            map.setCenter(last, animated: true)
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    final class Coordinator: NSObject, MKMapViewDelegate {
        var parent: MapKitMapView
        
        init(_ parent: MapKitMapView) {
            self.parent = parent
        }
        
        func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            if let polyline = overlay as? MKPolyline {
                let r = MKPolylineRenderer(polyline: polyline)
                r.lineWidth = 4
                r.strokeColor = .systemBlue
                return r
            }
            return MKOverlayRenderer()
        }

        func mapView(_ mapView: MKMapView, regionWillChangeAnimated animated: Bool) {
            // This logic detects if the change came from a user drag
            let view = mapView.subviews.first { $0.layer.animation(forKey: "MKMapViewBoundsAnimation") != nil }
            if view == nil {
                // Access parent property directly
                parent.locationManager.keepCentered = false
            }
        }
    }
}

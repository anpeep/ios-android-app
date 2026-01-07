//
//  MapKitMapView.swift
//  sport
//
//  Created by Anne-Riin Peep on 05.01.2026.
//
import CoreLocation



import MapKit
import SwiftUI
import MapKit
import SwiftUI

struct MapKitMapView: UIViewRepresentable {
    @ObservedObject var locationManager: LocationManager
    
    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView()
        map.delegate = context.coordinator
        map.showsUserLocation = true
        return map
    }
    func updateUIView(_ map: MKMapView, context: Context) {
        let points = locationManager.route
        
        // 1. Refresh Overlays (Lines)
        map.removeOverlays(map.overlays)
        
        // 2. Refresh Annotations (Markers)
        // We clear them first so we don't get duplicates
        map.removeAnnotations(map.annotations)
        
        // Add Special Markers (Start, End, CP, WP)
        for point in points {
            if let type = point.type, type != "LOC" {
                let annotation = MKPointAnnotation()
                annotation.coordinate = point.coordinate
                annotation.title = type
                map.addAnnotation(annotation)
            }
        }

        // 3. Draw Colored Segments
        guard points.count >= 2 else { return }
        
        for i in 0..<points.count - 1 {
            let segmentCoordinates = [points[i].coordinate, points[i+1].coordinate]
            let segmentPolyline = MKPolyline(coordinates: segmentCoordinates, count: 2)
            
            // Use the pace of the second point in the pair for coloring
            segmentPolyline.title = String(points[i+1].pace)
            
            map.addOverlay(segmentPolyline)
        }

        // 4. Centering Logic (Mainly for Live Tracking)
        if locationManager.keepCentered, let lastPoint = points.last {
            map.setCenter(lastPoint.coordinate, animated: true)
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
        
        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
                // Don't change the blue dot for user location
                if annotation is MKUserLocation { return nil }
                
                let identifier = "CustomAnnotation"
                var annotationView = mapView.dequeueReusableAnnotationView(withIdentifier: identifier)
                
                if annotationView == nil {
                    annotationView = MKAnnotationView(annotation: annotation, reuseIdentifier: identifier)
                    annotationView?.canShowCallout = true
                } else {
                    annotationView?.annotation = annotation
                }
                
                // Pick the image based on the title we set in updateUIView
                switch annotation.title {
                case "CP":
                    annotationView?.image = UIImage(named: "location")?.resized(to: CGSize(width: 32, height: 32))
                case "WP":
                    annotationView?.image = UIImage(named: "right-way")?.resized(to: CGSize(width: 32, height: 32))
                case "START":
                    annotationView?.image = UIImage(systemName: "play.circle.fill")?
                        .withTintColor(.systemGreen, renderingMode: .alwaysOriginal)
                case "END":
                    annotationView?.image = UIImage(systemName: "stop.circle.fill")?
                        .withTintColor(.systemRed, renderingMode: .alwaysOriginal)
                default:
                    return nil
                }
                
                return annotationView
            }
        
        func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            if let polyline = overlay as? MKPolyline {
                let renderer = MKPolylineRenderer(polyline: polyline)
                
                // Fix: Use parent.locationManager to access the color function
                if let paceString = polyline.title, let pace = Double(paceString) {
                    let color = parent.locationManager.color(for: pace)
                    renderer.strokeColor = UIColor(color)
                } else {
                    renderer.strokeColor = .systemBlue
                }
                
                renderer.lineWidth = 5
                return renderer
            }
            return MKOverlayRenderer(overlay: overlay)
        }
        
        func mapView(_ mapView: MKMapView, regionWillChangeAnimated animated: Bool) {
            // Correctly uses parent to toggle centering
            parent.locationManager.keepCentered = false
        }
    }
}
import UIKit

extension UIImage {
    func resized(to size: CGSize) -> UIImage {
        // Creates a graphics context with the target size
        return UIGraphicsImageRenderer(size: size).image { _ in
            // Draws the original image into the new smaller frame
            draw(in: CGRect(origin: .zero, size: size))
        }
    }
}

//
//  RealCompassView.swift
//  sport
//
//  Created by Anne-Riin Peep on 07.01.2026.
//
import SwiftUI
import Combine
struct RealCompassView: View {
    let degrees: Double
    
    var body: some View {
        ZStack {
            // The Compass Dial
            Circle()
                .stroke(Color.gray.opacity(0.2), lineWidth: 2)
                .frame(width: 100, height: 100)
            
            // Markers for N, S, E, W
            ForEach([0, 90, 180, 270], id: \.self) { marker in
                Text(markerName(marker))
                    .font(.system(size: 10, weight: .bold))
                    .offset(y: -40)
                    .rotationEffect(.degrees(Double(marker)))
            }
            
            // The Needle
            Capsule()
                .fill(.red)
                .frame(width: 4, height: 30)
                .offset(y: -15)
        }
        // This is the "magic": the whole dial rotates opposite to the device
        .rotationEffect(.degrees(-degrees))
        .background(Circle().fill(.ultraThinMaterial))
        .shadow(radius: 5)
    }
    
    func markerName(_ d: Int) -> String {
        if d == 0 { return "N" }
        if d == 90 { return "E" }
        if d == 180 { return "S" }
        return "W"
    }
}

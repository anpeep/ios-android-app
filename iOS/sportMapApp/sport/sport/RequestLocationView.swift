//
//  RequestLocationView.swift
//  sport
//
//  Created by Anne-Riin Peep on 05.01.2026.
//


import SwiftUI
import Combine

struct RequestLocationView: View {
    @EnvironmentObject var locationViewModel: LocationManager

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "location.circle")
                .resizable()
                .frame(width: 100, height: 100)
                .foregroundColor(.blue)

            Button {
                locationViewModel.requestPermission()
            } label: {
                Label("Allow tracking", systemImage: "location")
            }
            .padding()
            .background(Color.blue)
            .foregroundColor(.white)
            .clipShape(RoundedRectangle(cornerRadius: 10))

            Text("We need your permission to track location.")
                .font(.caption)
                .foregroundColor(.gray)
        }
    }
}

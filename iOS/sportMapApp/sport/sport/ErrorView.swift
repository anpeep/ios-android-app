//
//  ErrorView.swift
//  sport
//
//  Created by Anne-Riin Peep on 05.01.2026.
//


import SwiftUI

struct ErrorView: View {
    let errorText: String

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "xmark.octagon.fill")
                .resizable()
                .frame(width: 100, height: 100)
            Text(errorText)
                .multilineTextAlignment(.center)
        }
        .padding()
        .foregroundColor(.white)
        .background(Color.red)
    }
}

//
//  sportApp.swift
//  sport
//
//  Created by Anne-Riin Peep on 05.01.2026.
//
import SwiftUI

@main
struct sportApp: App {
    @StateObject var locationVM = LocationManager()
   @StateObject var accountVM = AccountViewModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(locationVM)
               .environmentObject(accountVM)
        }
    }
}

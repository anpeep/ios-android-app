//
//  sportApp.swift
//  sport
//
//  Created by Anne-Riin Peep on 05.01.2026.
//

import SwiftUI
import CoreData

@main
struct sportApp: App {
    let persistenceController = PersistenceController.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.managedObjectContext, persistenceController.container.viewContext)
        }
    }
}

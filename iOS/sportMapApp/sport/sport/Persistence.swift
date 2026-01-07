//
//  Persistence.swift
//  sport
//
//  Created by Anne-Riin Peep on 05.01.2026.
//
import CoreData

struct PersistenceController {
    static let shared = PersistenceController()

    // This preview section is used for SwiftUI Canvas/Previews
    @MainActor
    static let preview: PersistenceController = {
        let result = PersistenceController(inMemory: true)
        let viewContext = result.container.viewContext
        
        // Create a dummy location for the preview
        let newLocation = SavedLocation(context: viewContext)
        newLocation.timestamp = Date()
        newLocation.lat = 59.4370
        newLocation.lon = 24.7536
        newLocation.isSynced = false
        newLocation.sessionId = "preview-session"

        try? viewContext.save()
        return result
    }()

    let container: NSPersistentContainer

    init(inMemory: Bool = false) {
        // Must match your .xcdatamodeld filename exactly
        container = NSPersistentContainer(name: "sport")
        
        if inMemory {
            container.persistentStoreDescriptions.first!.url = URL(fileURLWithPath: "/dev/null")
        }
        
        container.loadPersistentStores(completionHandler: { (storeDescription, error) in
            if let error = error as NSError? {
                fatalError("Unresolved error \(error), \(error.userInfo)")
            }
        })
        container.viewContext.automaticallyMergesChangesFromParent = true
    }
}

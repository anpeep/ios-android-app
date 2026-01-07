//
//  LoginView.swift
//  sport
//
//  Created by Anne-Riin Peep on 06.01.2026.
//


import SwiftUI

struct LoginView: View {
    @EnvironmentObject var accountVM: AccountViewModel

    @State private var email = ""
    @State private var password = ""
    @State private var firstName = ""
    @State private var lastName = ""
    @State private var isRegister = false
    @State private var errorText: String?

    var body: some View {
        VStack(spacing: 16) {
            Text("SportMap")
                .font(.largeTitle)
                .bold()

            TextField("Email", text: $email)
                .textFieldStyle(.roundedBorder)
                .autocapitalization(.none)

            SecureField("Password", text: $password)
                .textFieldStyle(.roundedBorder)

            if isRegister {
                TextField("First name", text: $firstName)
                    .textFieldStyle(.roundedBorder)

                TextField("Last name", text: $lastName)
                    .textFieldStyle(.roundedBorder)
            }

            if let errorText {
                Text(errorText).foregroundColor(.red)
            }

            Button(isRegister ? "Create account" : "Login") {
                Task {
                    do {
                        if isRegister {
                            try await accountVM.register(
                                email: email,
                                password: password,
                                firstName: firstName,
                                lastName: lastName
                            )
                        } else {
                            try await accountVM.login(email: email, password: password)
                        }
                    } catch {
                        self.errorText = error.localizedDescription
                    }
                }
            }
            .buttonStyle(.borderedProminent)

            Button(isRegister ? "Already have account?" : "Create new account") {
                isRegister.toggle()
            }
        }
        .padding()
    }
}

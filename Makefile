.PHONY: build
build: | check-env
	-rm -r app/build
	./gradlew assembleRelease
	zipalign -v -p 4 \
		app/build/outputs/apk/release/app-release-unsigned.apk \
		app/build/outputs/apk/release/app-release-unsigned-aligned.apk
	apksigner sign \
		--ks "$(STORE_FILE)" \
		--ks-pass env:STORE_PASSWORD \
		--ks-key-alias "$(KEY_ALIAS)" \
		--key-pass env:KEY_PASSWORD \
		--out \
		app/build/outputs/apk/release/app-release.apk \
		app/build/outputs/apk/release/app-release-unsigned-aligned.apk

.PHONY: bundle
bundle: | check-env
	./gradlew bundleRelease \
		-Pandroid.injected.signing.store.file="$(STORE_FILE)" \
		-Pandroid.injected.signing.store.password="$(STORE_PASSWORD)" \
		-Pandroid.injected.signing.key.alias="$(KEY_ALIAS)" \
		-Pandroid.injected.signing.key.password="$(KEY_PASSWORD)"

.PHONY: check-env
check-env:
ifeq ($(STORE_FILE),)
	@echo "Variable STORE_FILE is not set."
	@echo "Example: STORE_FILE=path/to/keystore.js"
	exit 1
endif
ifeq ($(STORE_PASSWORD),)
	@echo "Variable STORE_PASSWORD is not set."
	@echo "Example: STORE_PASSWORD=mypassword"
	exit 1
endif
ifeq ($(KEY_ALIAS),)
	@echo "Variable KEY_ALIAS is not set."
	@echo "Example: KEY_ALIAS=com.example.android"
	exit 1
endif
ifeq ($(KEY_PASSWORD),)
	@echo "Variable KEY_PASSWORD is not set."
	@echo "Example: KEY_PASSWORD=mypassword"
	exit 1
endif

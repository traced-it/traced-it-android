.PHONY: build
build:
ifeq ($(keystore),)
	@echo "Usage: make build keystore=path/to/keystore.jks"
	exit 1
endif
	-rm -r app/build
	./gradlew assembleRelease
	zipalign -v -p 4 \
		app/build/outputs/apk/release/app-release-unsigned.apk \
		app/build/outputs/apk/release/app-release-unsigned-aligned.apk
	apksigner sign --ks "$(keystore)" --out \
		app/build/outputs/apk/release/app-release.apk \
		app/build/outputs/apk/release/app-release-unsigned-aligned.apk

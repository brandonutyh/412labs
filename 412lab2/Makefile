JAVAC = javac
SRC_DIR = src
BUILD_DIR = build
SOURCES = $(shell find $(SRC_DIR) -name "*.java")

# Java 8 compatibility
JFLAGS = -source 8 -target 8

.PHONY: clean build

build:
	@mkdir -p $(BUILD_DIR)
	$(JAVAC) $(JFLAGS) -d $(BUILD_DIR) $(SOURCES)

#Clean build
clean:
	rm -rf $(BUILD_DIR)

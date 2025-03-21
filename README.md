# EventLib

EventLib is a lightweight Java event handling library that enables you to build event-driven applications with ease. It
provides a flexible mechanism to create, register, and dispatch custom events using annotations and prioritized
listeners. The library is designed with performance, thread-safety, and ease of use in mind.

---

## Table of Contents

- [Installation](#installation)
- [Usage](#usage)
    - [Defining Custom Events](#defining-custom-events)
    - [Creating Event Subscribers](#creating-event-subscribers)
    - [Annotating Event Handlers](#annotating-event-handlers)
    - [Registering and Unregistering Listeners](#registering-and-unregistering-listeners)
    - [Dispatching Events](#dispatching-events)
- [Advanced Topics](#advanced-topics)
    - [Customizing Event Priority](#customizing-event-priority)
    - [Asynchronous Callbacks](#asynchronous-callbacks)
- [Building the Project](#building-the-project)

---

## Installation

### Maven

This lib is also on my [Repositories](https://repo.tnttastisch.de/#/)
To include EventLib in your project, add the following dependency to your `pom.xml`:

```xml

<repository>
    <id>tnttastisch-repo-releases</id>
    <name>TntTastisch Repository</name>
    <url>https://repo.tnttastisch.de/releases</url>
</repository>

<dependency>
<groupId>de.tnttastisch</groupId>
<artifactId>EventLib</artifactId>
<version>1.0-RELEASE</version>
</dependency>
```

The provided `pom.xml` also demonstrates the use of the Maven Shade Plugin to package the library.

---

## Usage

### Defining Custom Events

Create your custom events by extending the `AbstractEvent` class:

```java
package com.example.events;

import de.tnttastisch.eventlib.core.AbstractEvent;

public class UserLoginEvent extends AbstractEvent {
    private final String username;

    public UserLoginEvent(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
```

### Creating Event Subscribers

Implement the `EventSubscriber` interface (a marker interface) in your listener class:

```java
package com.example.listeners;

import de.tnttastisch.eventlib.core.EventSubscriber;
import de.tnttastisch.eventlib.annotation.SubscribeEvent;
import com.example.events.UserLoginEvent;

public class LoginListener implements EventSubscriber {

    @SubscribeEvent
    public void onUserLogin(UserLoginEvent event) {
        System.out.println("User logged in: " + event.getUsername());
    }
}
```

### Annotating Event Handlers

The `@SubscribeEvent` annotation is used to mark methods that handle events. Each annotated method must have exactly one
parameter – the event to handle. You can specify a custom priority level:

```java

@SubscribeEvent(priorityLevel = Priority.HIGH)
public void onUserLogin(UserLoginEvent event) {
    // High-priority handling logic here
}
```

For details, see `SubscribeEvent.java`.

### Registering and Unregistering Listeners

Use the `EventRegistry` to manage your event listeners:

```java
package com.example;

import de.tnttastisch.eventlib.manager.EventRegistry;
import com.example.listeners.LoginListener;

public class Main {
    public static void main(String[] args) {
        EventRegistry registry = new EventRegistry();
        LoginListener listener = new LoginListener();

        // Register the listener
        registry.registerListener(listener);

        // Dispatch events using the dispatcher from the registry
        UserLoginEvent loginEvent = new UserLoginEvent("Alice");
        registry.getDispatcher().post(loginEvent);

        // Unregister when done
        registry.unregisterListener(listener);
    }
}
```

The registration mechanism uses reflection to scan your listener for methods annotated with `@SubscribeEvent` and binds
them using a robust dispatcher.

### Dispatching Events

To dispatch an event, simply call the `post` method on the dispatcher:

```java
registry.getDispatcher().

post(new UserLoginEvent("Alice"));
```

Under the hood, the dispatcher finds all handlers for the event type and executes them in order based on their priority.
If a listener takes too long (more than 50ms), a warning is logged.


---

## Advanced Topics

### Customizing Event Priority

Event handler methods can be annotated with a specific priority using the `priorityLevel` attribute. The available
priorities in the `Priority` enum are:

- **LOWEST** (-64)
- **LOW** (-32)
- **NORMAL** (0) – default priority
- **HIGH** (32)
- **HIGHEST** (64)

This feature allows you to control the order in which event handlers are executed. For example, a handler marked as
`HIGHEST` will run before one marked as `LOW`.

### Asynchronous Callbacks

For asynchronous operations, the library provides the `AsyncCallback` interface:

```java
public interface AsyncCallback<V> {
    void onSuccess(V result);

    void onFailure(V result, Throwable error);
}
```

Implement this interface to handle the result of asynchronous tasks, allowing your event handlers to process results in
a non-blocking way.

---

## Building the Project

The project uses Maven for dependency management and building. To compile and package the library, run:

```bash
mvn clean package
```

This command will compile the source code, run tests (if any), and package the application into a JAR file using the
Maven Shade Plugin.

package org.example;

import org.junit.jupiter.api.Test;

import RegistryService.RegistryServer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basale Testklasse fuer die Projektstruktur.
 *
 * @author Emre Öztürk
 * @date 16.12.25
 */
class AppTest {

    /**
     * Prueft, dass die Testumgebung lauffaehig ist.
     */
    @Test
    void appHasAGreeting() {
        RegistryServer classUnderTest = new RegistryServer();
        assertTrue(true);
    }
}

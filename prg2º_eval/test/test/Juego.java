package test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import armas.Arma;
import estado.Estado;
import hechizos.HechizoCuracion;
import hechizos.HechizoDañoDirecto;
import hechizos.HechizoDot;
import hechizos.HechizoHot;
import personaje.Mercenario; 
import personaje.Personaje;

public class Juego {

    private Personaje atacante;
    private Personaje objetivo;
    private Arma      armaCaC;       // cuerpo a cuerpo
    private Arma      armaDistancia; // a distancia

  
    @BeforeEach
    void setUp() {
        armaCaC       = new Arma("Katana",  15, true);   // esCuerpoACuerpo = true
        armaDistancia = new Arma("Pistola",  8, false);  // esCuerpoACuerpo = false

        // Personaje con arma a distancia (siempre puede atacar)
        atacante = new Mercenario("Atacante");
        objetivo = new Mercenario("Objetivo");
    }

    // BLOQUE 1  VIDA Y DAÑO

    @Test
    @DisplayName("recibirDaño: vida se reduce correctamente")
    void testRecibirDañoReduceVida() {
        int vidaAntes = objetivo.getVidaActual();
        objetivo.recibirDaño(30);
        assertEquals(vidaAntes - 30, objetivo.getVidaActual(),
                "La vida debería reducirse exactamente en el daño recibido");
    }

    @Test
    @DisplayName("recibirDaño: la vida nunca baja de 0")
    void testRecibirDañoNuncaNegativo() {
        objetivo.recibirDaño(99999); // daño brutal
        assertEquals(0, objetivo.getVidaActual(),
                "La vida no puede ser negativa, mínimo es 0");
    }

    @Test
    @DisplayName("curar: la vida aumenta correctamente")
    void testCurarAumentaVida() {
        objetivo.recibirDaño(50);        // bajar vida primero
        int vidaAntes = objetivo.getVidaActual();
        objetivo.curar(20);
        assertEquals(vidaAntes + 20, objetivo.getVidaActual(),
                "La curación debería sumar exactamente los puntos indicados");
    }

    @Test
    @DisplayName("curar: la vida nunca supera el máximo")
    void testCurarNuncaSuperaMaximo() {
        objetivo.curar(99999); // curación brutal
        assertEquals(objetivo.getVidaMax(), objetivo.getVidaActual(),
                "La vida no puede superar el máximo");
    }

    @Test
    @DisplayName("estaVivo: devuelve false cuando la vida llega a 0")
    void testEstaVivoFalseCon0HP() {
        objetivo.recibirDaño(99999);
        assertFalse(objetivo.estaVivo(),
                "Un personaje con 0 HP no debería estar vivo");
    }

    @Test
    @DisplayName("estaVivo: devuelve true mientras tenga vida")
    void testEstaVivoTrueConVida() {
        assertTrue(objetivo.estaVivo(),
                "Un personaje recién creado debería estar vivo");
    }

    @Test
    @DisplayName("setVidaActual: no acepta valores por encima del máximo")
    void testSetVidaActualNoSuperaMaximo() {
        objetivo.setVidaActual(99999);
        assertEquals(objetivo.getVidaMax(), objetivo.getVidaActual(),
                "setVidaActual debe clampear al máximo si el valor es mayor");
    }

    @Test
    @DisplayName("setVidaActual: no acepta valores negativos")
    void testSetVidaActualNoNegativo() {
        objetivo.setVidaActual(-50);
        assertEquals(0, objetivo.getVidaActual(),
                "setVidaActual debe clampear a 0 si el valor es negativo");
    }

    // BLOQUE 2  ARMAS Y ATAQUE FÍSICO

    @Test
    @DisplayName("Arma CaC: calcularDaño añade +5 de bonus")
    void testArmaCaCTieneBonusDaño() {
        assertEquals(20, armaCaC.calcularDaño(),
                "Un arma CaC de dañoBase 15 debería hacer 15+5 = 20");
    }

    @Test
    @DisplayName("Arma a distancia: calcularDaño no tiene bonus")
    void testArmaDistanciaSinBonus() {
        assertEquals(8, armaDistancia.calcularDaño(),
                "Un arma a distancia de dañoBase 8 debería hacer exactamente 8");
    }

    @Test
    @DisplayName("intentarAtacarConArma: arma CaC falla si está lejos")
    void testAtaqueCaCFallaLejos() {
        // Creamos un personaje con arma CaC y estaCerca = false (por defecto)
        Personaje luchador = new Personaje("Luchador", 100, 50, armaCaC) {};
        boolean resultado = luchador.intentarAtacarConArma(objetivo);
        assertFalse(resultado,
                "Un arma CaC no debería poder usarse si el personaje está lejos");
    }

    @Test
    @DisplayName("intentarAtacarConArma: arma a distancia siempre funciona")
    void testAtaqueDistanciaFuncionaSiempre() {
        Personaje tirador = new Personaje("Tirador", 100, 50, armaDistancia) {};
        boolean resultado = tirador.intentarAtacarConArma(objetivo);
        assertTrue(resultado,
                "Un arma a distancia debería poder usarse siempre, sin importar la proximidad");
    }

    @Test
    @DisplayName("intentarAtacarConArma: el objetivo recibe daño real")
    void testAtaqueReduceVidaObjetivo() {
        Personaje tirador = new Personaje("Tirador", 100, 50, armaDistancia) {};
        int vidaAntes = objetivo.getVidaActual();
        tirador.intentarAtacarConArma(objetivo);
        assertTrue(objetivo.getVidaActual() < vidaAntes,
                "El objetivo debe haber recibido daño tras el ataque");
    }

    // BLOQUE 3  HECHIZOS

    @Test
    @DisplayName("HechizoCuracion: cura correctamente al objetivo")
    void testHechizoCuracionCura() {
        objetivo.recibirDaño(40); // bajar vida antes de curar
        int vidaAntes = objetivo.getVidaActual();
        HechizoCuracion curacion = new HechizoCuracion("Sanación", 10, 25);
        atacante.setEnergia(50);
        curacion.lanzar(atacante, objetivo);
        assertEquals(vidaAntes + 25, objetivo.getVidaActual(),
                "La curación debería restaurar exactamente 25 HP");
    }

    @Test
    @DisplayName("HechizoCuracion: falla si no hay energía suficiente")
    void testHechizoCuracionSinEnergia() {
        atacante.setEnergia(0); // sin energía
        HechizoCuracion curacion = new HechizoCuracion("Sanación", 20, 25);
        boolean resultado = curacion.lanzar(atacante, objetivo);
        assertFalse(resultado,
                "El hechizo no debería lanzarse si no hay energía suficiente");
    }

    @Test
    @DisplayName("HechizoCuracion: descuenta la energía correctamente")
    void testHechizoCuracionDescuentaEnergia() {
        atacante.setEnergia(50);
        HechizoCuracion curacion = new HechizoCuracion("Sanación", 20, 25);
        objetivo.recibirDaño(40);
        curacion.lanzar(atacante, objetivo);
        assertEquals(30, atacante.energia,
                "Tras lanzar el hechizo de coste 20, la energía debería ser 30");
    }

    @Test
    @DisplayName("HechizoDañoDirecto: reduce la vida del objetivo")
    void testHechizoDañoDirectoReduceVida() {
        atacante.setEnergia(50);
        int vidaAntes = objetivo.getVidaActual();
        HechizoDañoDirecto daño = new HechizoDañoDirecto("Bola de Fuego", 20, 35);
        daño.lanzar(atacante, objetivo);
        assertEquals(vidaAntes - 35, objetivo.getVidaActual(),
                "El hechizo de daño directo debería quitar exactamente 35 HP");
    }

    @Test
    @DisplayName("HechizoDañoDirecto: falla si no hay energía")
    void testHechizoDañoDirectoSinEnergia() {
        atacante.setEnergia(5); // menos que el coste
        HechizoDañoDirecto daño = new HechizoDañoDirecto("Bola de Fuego", 20, 35);
        boolean resultado = daño.lanzar(atacante, objetivo);
        assertFalse(resultado,
                "El hechizo no debería lanzarse sin energía suficiente");
    }

    @Test
    @DisplayName("HechizoDot: aplica un estado de daño al objetivo")
    void testHechizoDotAplicaEstado() {
        atacante.setEnergia(50);
        HechizoDot dot = new HechizoDot("Veneno", 10, "Veneno", 8);
        dot.lanzar(atacante, objetivo);
        // Verificamos indirectamente: procesarEstados debe reducir la vida
        int vidaAntes = objetivo.getVidaActual();
        objetivo.procesarEstados();
        assertTrue(objetivo.getVidaActual() < vidaAntes,
                "Tras aplicar DoT y procesar estados, el objetivo debería haber recibido daño");
    }

    @Test
    @DisplayName("HechizoHot: aplica un estado de curación al objetivo")
    void testHechizoHotAplicaEstado() {
        objetivo.recibirDaño(50); // bajar vida
        atacante.setEnergia(50);
        HechizoHot hot = new HechizoHot("Regeneración", 15, "Regen", 10);
        hot.lanzar(atacante, objetivo);
        int vidaAntes = objetivo.getVidaActual();
        objetivo.procesarEstados();
        assertTrue(objetivo.getVidaActual() > vidaAntes,
                "Tras aplicar HoT y procesar estados, el objetivo debería haberse curado");
    }

    // BLOQUE 4 — ESTADOS ACTIVOS (DOT / HOT)

    @Test
    @DisplayName("Estado DoT: reduce la vida al procesarse")
    void testEstadoDotDañaPorTurno() {
        Estado veneno = new Estado("Veneno", 10, 3, true); // esDaño = true
        objetivo.agregarEstado(veneno);
        int vidaAntes = objetivo.getVidaActual();
        objetivo.procesarEstados();
        assertEquals(vidaAntes - 10, objetivo.getVidaActual(),
                "El DoT debería quitar 10 HP al procesarse");
    }

    @Test
    @DisplayName("Estado HoT: aumenta la vida al procesarse")
    void testEstadoHotCuraPorTurno() {
        objetivo.recibirDaño(50);
        Estado regen = new Estado("Regen", 10, 3, false); // esDaño = false
        objetivo.agregarEstado(regen);
        int vidaAntes = objetivo.getVidaActual();
        objetivo.procesarEstados();
        assertEquals(vidaAntes + 10, objetivo.getVidaActual(),
                "El HoT debería curar 10 HP al procesarse");
    }

    @Test
    @DisplayName("Estado: se elimina automáticamente al agotar su duración")
    void testEstadoSeEliminaAlTerminar() {
        Estado veneno = new Estado("Veneno", 5, 1, true); // duración = 1 turno
        objetivo.agregarEstado(veneno);
        objetivo.procesarEstados(); // turno 1: aplica y deja duración en 0 → se elimina
        objetivo.procesarEstados(); // turno 2: no debería aplicar nada
        // Si el estado se eliminó, la vida del turno 2 no habrá cambiado
        int vidaTrasTurno2 = objetivo.getVidaActual();
        objetivo.procesarEstados(); // turno 3: tampoco debería cambiar nada
        assertEquals(vidaTrasTurno2, objetivo.getVidaActual(),
                "El estado debería haberse eliminado tras agotar su duración");
    }

    @Test
    @DisplayName("agregarEstado: no apila dos estados iguales, renueva la duración")
    void testAgregarEstadoNoDuplica() {
        Estado veneno1 = new Estado("Veneno", 10, 3, true);
        Estado veneno2 = new Estado("Veneno", 10, 5, true); // mismo nombre, más duración
        objetivo.agregarEstado(veneno1);
        objetivo.agregarEstado(veneno2); // debería renovar, no duplicar
        objetivo.procesarEstados();      // si hubiera duplicado, haría 20 de daño
        int vidaAntes = objetivo.getVidaActual();
        // Solo debería haber un estado activo haciendo 10 de daño por turno
        objetivo.procesarEstados();
        assertEquals(vidaAntes - 10, objetivo.getVidaActual(),
                "Solo debe existir un estado con ese nombre, no debe duplicarse");
    }

    @Test
    @DisplayName("haTerminado: devuelve true cuando la duración llega a 0")
    void testHaTerminadoTrue() {
        Estado estado = new Estado("Test", 5, 1, true);
        estado.aplicarEfecto(objetivo); // reduce duración a 0
        assertTrue(estado.haTerminado(),
                "El estado debería haber terminado tras agotar su duración");
    }

    @Test
    @DisplayName("haTerminado: devuelve false si aún quedan turnos")
    void testHaTerminadoFalse() {
        Estado estado = new Estado("Test", 5, 3, true);
        assertFalse(estado.haTerminado(),
                "El estado no debería haber terminado con 3 turnos restantes");
    }}
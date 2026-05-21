package juego;
 
import personaje.Personaje;
import personaje.Mercenario;
import personaje.Netrunner;
import personaje.Doctor;
import basededatos.GestorBD;
import java.util.ArrayList;
import graficos.GraficosXchart;
 
public class Juego {
 
    private ArrayList<Personaje> tuEquipo;
    private ArrayList<Personaje> enemigos;

    // Guardamos cuántas rondas duró el combate para poder informar
    // al ranking cuando iniciar  termine. Main lo lee con getRondasJugadas().
    private int rondasJugadas = 0;
 
    public Juego() {
        tuEquipo = new ArrayList<>();
        tuEquipo.add(new Mercenario("Rex"));
        tuEquipo.add(new Netrunner("Zara"));
        tuEquipo.add(new Doctor("Patch"));
 
        enemigos = new ArrayList<>();
        enemigos.add(new Mercenario("Security-Droid"));
        enemigos.add(new Netrunner("Black-Hat IA"));
        enemigos.add(new Doctor("Drone-Reparador"));
    }
 
    public void guardarPersonajesIniciales(int idPartida) {
        for (Personaje p : tuEquipo) {
            GestorBD.guardarPersonaje(idPartida, p.getNombre(),
                    p.getClass().getSimpleName(),
                    p.getVidaActual(), p.getVidaMax(), p.energia, true);
        }
        for (Personaje p : enemigos) {
            GestorBD.guardarPersonaje(idPartida, p.getNombre(),
                    p.getClass().getSimpleName(),
                    p.getVidaActual(), p.getVidaMax(), p.energia, false);
        }

        GestorBD.registrarEvento(idPartida, 0, "INICIO_PARTIDA",
                "Partida creada con " + tuEquipo.size() + " héroes y "
                + enemigos.size() + " enemigos");
    }
 
    public void cargarDesdeBD(int idPartida) {
        this.tuEquipo = GestorBD.cargarPersonajesPorBando(idPartida, true);
        this.enemigos = GestorBD.cargarPersonajesPorBando(idPartida, false);
 
        if (tuEquipo.isEmpty() || enemigos.isEmpty()) {
            System.out.println("[ERROR] No se encontraron personajes para la partida " + idPartida);
        } else {
            System.out.println("[OK] Partida " + idPartida + " cargada correctamente.");
        }
    }
 
    public void mostrarEquipos() {
        System.out.println("\n   TU ESCUADRON:");
        for (Personaje p : tuEquipo) System.out.println("   + " + p.getEstado());
        System.out.println("\n   ENEMIGOS:");
        for (Personaje p : enemigos) System.out.println("   - " + p.getEstado());
    }

    /**
     * Devuelve cuántas rondas duró el último combate.
     * Main lo llama después de iniciar() para pasárselo a actualizarRanking().
     */
    public int getRondasJugadas() {
        return rondasJugadas;
    }
 
    /*
     * Bucle principal de combate.
     * devuelve boolean:
     *   true  → el jugador ganó (todos los enemigos eliminados)
     *   false → el jugador perdió (todos sus héroes cayeron)
     * Main captura este valor para saber qué registrar en el ranking.
     */
    public boolean iniciar(int idPartida) throws InterruptedException {
        int ronda = 1;
 
        while (true) {
            System.out.println("\n");
            linea('=', 60);
            System.out.println("   RONDA " + ronda);
            linea('=', 60);

            GestorBD.registrarEvento(idPartida, ronda, "INICIO_RONDA",
                    "Comienza la ronda " + ronda);
 
            System.out.println("   TU EQUIPO:");
            for (Personaje p : tuEquipo)
                System.out.println("   " + (p.estaVivo() ? "  " : "X ") + p.getEstado());
            System.out.println("   ENEMIGOS:");
            for (Personaje p : enemigos)
                System.out.println("   " + (p.estaVivo() ? "  " : "X ") + p.getEstado());
 
            // Turno de tu equipo 
            System.out.println("\n  --- Turno de tu equipo ---");
            for (Personaje heroe : tuEquipo) {
                if (!heroe.estaVivo()) continue;

                System.out.println("\n  >> " + heroe.getNombre());

                GestorBD.registrarEvento(idPartida, ronda, "TURNO_HEROE",
                        heroe.getNombre() + " (" + heroe.getClass().getSimpleName()
                        + ") actúa | HP: " + heroe.getVidaActual()
                        + "/" + heroe.getVidaMax() + " | EN: " + heroe.energia);

                heroe.procesarEstados();
                if (heroe.estaVivo()) heroe.actuar(enemigos, tuEquipo);
                Thread.sleep(1000);

                for (Personaje e : enemigos) {
                    if (!e.estaVivo()) {
                        registrarMuerteSiNoEstaba(idPartida, ronda, e, "MUERTE_ENEMIGO");
                    }
                }

                if (todosDerrota(enemigos)) break;
            }
 
            if (todosDerrota(enemigos)) {
                linea('=', 60);
                System.out.println("   MISION CUMPLIDA. Enemigos neutralizados!");
                linea('=', 60);

                GestorBD.registrarEvento(idPartida, ronda, "VICTORIA",
                        "¡Misión cumplida! Todos los enemigos eliminados en la ronda " + ronda);

                // Guardamos las rondas jugadas para que Main pueda leerlas
                rondasJugadas = ronda;

                // ── XCHART: mostramos las gráficas al ganar ──────────────
                GraficosXchart.mostrarEstadisticas();

                // Devolvemos true = victoria
                return true;
            }
 
            //  Turno enemigo 
            System.out.println("\n  --- Turno enemigo ---");
            for (Personaje enemigo : enemigos) {
                if (!enemigo.estaVivo()) continue;

                System.out.println("\n  >> " + enemigo.getNombre());

                GestorBD.registrarEvento(idPartida, ronda, "TURNO_ENEMIGO",
                        enemigo.getNombre() + " (" + enemigo.getClass().getSimpleName()
                        + ") actúa | HP: " + enemigo.getVidaActual()
                        + "/" + enemigo.getVidaMax() + " | EN: " + enemigo.energia);

                enemigo.procesarEstados();
                if (enemigo.estaVivo()) enemigo.actuar(tuEquipo, enemigos);
                Thread.sleep(800);

                for (Personaje heroe : tuEquipo) {
                    if (!heroe.estaVivo()) {
                        registrarMuerteSiNoEstaba(idPartida, ronda, heroe, "MUERTE_ALIADO");
                    }
                }

                if (todosDerrota(tuEquipo)) break;
            }
 
            if (todosDerrota(tuEquipo)) {
                linea('=', 60);
                System.out.println("   DERROTA. Tu escuadron ha caido.");
                linea('=', 60);

                GestorBD.registrarEvento(idPartida, ronda, "DERROTA",
                        "El escuadrón ha caído en la ronda " + ronda);

                // Guardamos las rondas jugadas para que Main pueda leerlas
                rondasJugadas = ronda;

                // ── XCHART: mostramos las gráficas al perder ─────────────
                GraficosXchart.mostrarEstadisticas();

                // Devolvemos false = derrota
                return false;
            }

            System.out.println("\n   [Guardando progreso de la ronda " + ronda + " en MySQL...]");
            for (Personaje p : tuEquipo) {
                GestorBD.actualizarPersonaje(idPartida, p.getNombre(), p.getVidaActual(), p.energia);
            }
            for (Personaje p : enemigos) {
                GestorBD.actualizarPersonaje(idPartida, p.getNombre(), p.getVidaActual(), p.energia);
            }
            GestorBD.actualizarTurno(idPartida, ronda);
 
            ronda++;
            Thread.sleep(1000);
        }
    }

    /*
     * Registra la muerte de un personaje solo la primera vez.
     * Consulta la BD antes de insertar para evitar duplicados.
     */
    private void registrarMuerteSiNoEstaba(int idPartida, int ronda,
                                           Personaje personaje, String tipoEvento) {
        String sqlCheck = "SELECT COUNT(*) FROM historial_partida "
                        + "WHERE id_partida = ? AND tipo_evento = ? "
                        + "AND descripcion LIKE ?";

        try (java.sql.Connection con = GestorBD.conectar();
             java.sql.PreparedStatement ps = con.prepareStatement(sqlCheck)) {

            ps.setInt(1, idPartida);
            ps.setString(2, tipoEvento);
            ps.setString(3, "%" + personaje.getNombre() + "%");

            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                GestorBD.registrarEvento(idPartida, ronda, tipoEvento,
                        personaje.getNombre() + " (" + personaje.getClass().getSimpleName()
                        + ") ha sido eliminado en la ronda " + ronda);
            }
        } catch (java.sql.SQLException e) { e.printStackTrace(); }
    }
 
    private boolean todosDerrota(ArrayList<Personaje> equipo) {
        for (Personaje p : equipo) {
            if (p.estaVivo()) return false;
        }
        return true;
    }
 
    private void linea(char c, int n) {
        System.out.println(String.valueOf(c).repeat(n));
    }
}
package basededatos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import personaje.Personaje;
import personaje.Mercenario;
import personaje.Netrunner;
import personaje.Doctor;

public class GestorBD {

    private static final String URL      = "jdbc:mysql://localhost:3306/rpg_game";
    private static final String USUARIO  = "root";
    private static final String PASSWORD = "";

    public static Connection conectar() {
        Connection conexion = null;
        try {
            conexion = DriverManager.getConnection(URL, USUARIO, PASSWORD);
        } catch (SQLException e) {
            System.out.println("Error al conectar con la base de datos.");
            e.printStackTrace();
        }
        return conexion;
    }

    public static int crearNuevaPartida(String nombreJugador) {
        String sql = "INSERT INTO partidas (nombre_jugador, turno_actual) VALUES (?, 1)";
        int idPartidaGenerado = -1;

        try (Connection conexion = conectar();
             PreparedStatement pstmt = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, nombreJugador);
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                idPartidaGenerado = rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }

        return idPartidaGenerado;
    }

    public static void guardarPersonaje(int idPartida, String nombre, String clasePersonaje,
                                        int vidaActual, int vidaMax, int energia, boolean esAliado) {
        String sql = "INSERT INTO personajes_partida (id_partida, nombre, clase, vida_actual, vida_max, energia, es_aliado) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conexion = conectar();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idPartida);
            pstmt.setString(2, nombre);
            pstmt.setString(3, clasePersonaje);
            pstmt.setInt(4, vidaActual);
            pstmt.setInt(5, vidaMax);
            pstmt.setInt(6, energia);
            pstmt.setBoolean(7, esAliado);
            pstmt.executeUpdate();

        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void listarPartidas() {
        String sql = "SELECT * FROM partidas";
        try (Connection conexion = conectar();
             PreparedStatement pstmt = conexion.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("\n=== PARTIDAS GUARDADAS ===");
            boolean hayPartidas = false;

            while (rs.next()) {
                hayPartidas = true;
                System.out.println("ID: " + rs.getInt("id_partida")
                        + " | Jugador: " + rs.getString("nombre_jugador")
                        + " | Turno: "   + rs.getInt("turno_actual"));
            }
            if (!hayPartidas) {
                System.out.println("No hay partidas guardadas.");
            }
            System.out.println("====================\n");

        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static ArrayList<Personaje> cargarPersonajesPorBando(int idPartida, boolean esAliado) {
        ArrayList<Personaje> lista = new ArrayList<>();
        String sql = "SELECT nombre, clase, vida_actual, vida_max, energia "
                   + "FROM personajes_partida "
                   + "WHERE id_partida = ? AND es_aliado = ?";

        try (Connection conexion = conectar();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idPartida);
            pstmt.setBoolean(2, esAliado);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String nombre     = rs.getString("nombre");
                String clase      = rs.getString("clase");
                int    vidaActual = rs.getInt("vida_actual");
                int    energia    = rs.getInt("energia");

                Personaje p = null;

                switch (clase) {
                    case "Mercenario": p = new Mercenario(nombre); break;
                    case "Netrunner":  p = new Netrunner(nombre);  break;
                    case "Doctor":     p = new Doctor(nombre);     break;
                    default:
                        System.out.println("Clase desconocida en BD: " + clase);
                        break;
                }

                if (p != null) {
                    p.setVidaActual(vidaActual);
                    p.setEnergia(energia);
                    lista.add(p);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        return lista;
    }

    public static void actualizarPersonaje(int idPartida, String nombre, int vidaActual, int energia) {
        String sql = "UPDATE personajes_partida SET vida_actual = ?, energia = ? "
                   + "WHERE id_partida = ? AND nombre = ?";
        try (Connection conexion = conectar();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, vidaActual);
            pstmt.setInt(2, energia);
            pstmt.setInt(3, idPartida);
            pstmt.setString(4, nombre);
            pstmt.executeUpdate();

        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void actualizarTurno(int idPartida, int turnoActual) {
        String sql = "UPDATE partidas SET turno_actual = ? WHERE id_partida = ?";
        try (Connection conexion = conectar();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, turnoActual);
            pstmt.setInt(2, idPartida);
            pstmt.executeUpdate();

        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void borrarPartida(int idPartida) {
        String sql = "DELETE FROM partidas WHERE id_partida = ?";
        try (Connection conexion = conectar();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idPartida);
            int filasBorradas = pstmt.executeUpdate();

            if (filasBorradas > 0) {
                System.out.println("Partida " + idPartida + " borrada correctamente.");
            } else {
                System.out.println("No se encontró la partida con ID " + idPartida + ".");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /*
     * Inserta un evento en la tabla historial partida 
     */
    public static void registrarEvento(int idPartida, int ronda,
                                       String tipoEvento, String descripcion) {
        String sql = "INSERT INTO historial_partida (id_partida, ronda, tipo_evento, descripcion) "
                   + "VALUES (?, ?, ?, ?)";

        try (Connection conexion = conectar();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idPartida);
            pstmt.setInt(2, ronda);
            pstmt.setString(3, tipoEvento);
            pstmt.setString(4, descripcion);
            pstmt.executeUpdate();

        } catch (SQLException e) { e.printStackTrace(); }
    }

    /*
     * Muestra todos los eventos de una partida ordenados cronológicamente.
     */
    public static void mostrarHistorial(int idPartida) {
        String sql = "SELECT ronda, tipo_evento, descripcion, fecha_hora "
                   + "FROM historial_partida "
                   + "WHERE id_partida = ? "
                   + "ORDER BY id_historial ASC";

        try (Connection conexion = conectar();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idPartida);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n========== HISTORIAL DE LA PARTIDA " + idPartida + " ==========");
            boolean hayEventos = false;

            while (rs.next()) {
                hayEventos = true;
                System.out.printf("[Ronda %-2d | %-18s | %s] %s%n",
                        rs.getInt("ronda"),
                        rs.getString("tipo_evento"),
                        rs.getString("fecha_hora"),
                        rs.getString("descripcion"));
            }

            if (!hayEventos) {
                System.out.println("No hay eventos registrados para esta partida.");
            }
            System.out.println("================================\n");

        } catch (SQLException e) { e.printStackTrace(); }
    }

    //  RANKING GLOBAL 

    public static void actualizarRanking(String nombreJugador, boolean victoria, int rondasJugadas) {

        // Convertimos el boolean a enteros para poder sumarlos directamente en SQL
        int sumaVictorias = victoria ? 1 : 0; // 1 si ganó, 0 si perdió
        int sumaDerrotas  = victoria ? 0 : 1; // 0 si ganó, 1 si perdió

        String sql = "INSERT INTO ranking "
                   + "    (nombre_jugador, victorias, derrotas, partidas_jugadas, rondas_totales) "
                   + "VALUES (?, ?, ?, 1, ?) "
                   + "ON DUPLICATE KEY UPDATE "
                   + "    victorias        = victorias        + VALUES(victorias),       "
                   + "    derrotas         = derrotas         + VALUES(derrotas),        "
                   + "    partidas_jugadas = partidas_jugadas + 1,                       "
                   + "    rondas_totales   = rondas_totales   + VALUES(rondas_totales)  ";

        try (Connection conexion = conectar();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setString(1, nombreJugador); // nombre del jugador
            pstmt.setInt(2, sumaVictorias);    // victorias de esta partida (0 o 1)
            pstmt.setInt(3, sumaDerrotas);     // derrotas de esta partida (0 o 1)
            pstmt.setInt(4, rondasJugadas);    // rondas que duró esta partida
            pstmt.executeUpdate();

            // Confirmamos por consola qué se ha guardado
            String resultado = victoria ? "VICTORIA" : "DERROTA";
            System.out.println("\n   [Ranking actualizado → " + nombreJugador
                    + ": " + resultado + " en " + rondasJugadas + " rondas]");

        } catch (SQLException e) { e.printStackTrace(); }
    }

    /*
     * Muestra el ranking global ordenado por victorias (mayor a menor).
     * Si dos jugadores empatan en victorias, gana el que termina
     * antes de media (menos rondas por partida).
     * ROUND(rondas_totales / partidas_jugadas, 1) calcula la media
     * con 1 decimal directamente en SQL.
     */
    public static void mostrarRanking() {

        String sql = "SELECT nombre_jugador, victorias, derrotas, partidas_jugadas, "
                   + "       ROUND(rondas_totales / partidas_jugadas, 1) AS media_rondas "
                   + "FROM ranking "
                   + "ORDER BY victorias DESC, media_rondas ASC"; // más victorias primero;
                                                                   // empate → menos rondas de media

        try (Connection conexion = conectar();
             PreparedStatement pstmt = conexion.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            // Cabecera de la tabla con caracteres de borde
            System.out.println("\n╔══════════════════════════════════════════════════════╗");
            System.out.println("║           RANKING GLOBAL DE JUGADORES                ║");
            System.out.println("╠═══╦══════════════════╦═════╦══════╦════════╦════════╣");
            System.out.printf ("║ %-2s║ %-17s║ %-4s║ %-5s║ %-7s║ %-7s║%n",
                    "#", "Jugador", "Vic.", "Der.", "Partidas", "Med.Ron");
            System.out.println("╠═══╬══════════════════╬═════╬══════╬════════╬════════╣");

            int posicion = 1;
            boolean hayJugadores = false;

            while (rs.next()) {
                hayJugadores = true;

                String nombre      = rs.getString("nombre_jugador");
                int    victorias   = rs.getInt("victorias");
                int    derrotas    = rs.getInt("derrotas");
                int    partidas    = rs.getInt("partidas_jugadas");
                double mediaRondas = rs.getDouble("media_rondas");

                if (nombre.length() > 17) nombre = nombre.substring(0, 17);

                System.out.printf("║ %-2d║ %-17s║ %-4d║ %-5d║ %-7d║ %-7.1f║%n",
                        posicion, nombre, victorias, derrotas, partidas, mediaRondas);
                posicion++;
            }

            if (!hayJugadores) {
                System.out.println("║      Aún no hay jugadores en el ranking.             ║");
            }

            System.out.println("╚═══╩══════════════════╩═════╩══════╩════════╩════════╝\n");

        } catch (SQLException e) { e.printStackTrace(); }
    }

    /*
     * Recupera el nombre del jugador propietario de una partida desde la BD.
     * Se usa al cargar una partida guardada para saber a quién actualizar
     * en el ranking cuando el combate termine.
     * Devuelve null si el ID no existe.
     */
    public static String getNombreJugador(int idPartida) {
        String sql = "SELECT nombre_jugador FROM partidas WHERE id_partida = ?";
        try (Connection conexion = conectar();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idPartida);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("nombre_jugador"); // Devuelve el nombre encontrado
            }
        } catch (SQLException e) { e.printStackTrace(); }

        return null; // No se encontró ninguna partida con ese ID
    }
}
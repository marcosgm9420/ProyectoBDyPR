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

    // CONFIGURACIÓN DE CONEXIÓN 
 
    private static final String URL      = "jdbc:mysql://localhost:3306/rpg_game";
    private static final String USUARIO  = "root";
    private static final String PASSWORD = "";

    // CONEXIÓN 

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

    // CRUD: CREATE 

    /*
     * Crea una nueva fila en la tabla 'partidas' y devuelve el ID que MySQL
     * generó automáticamente 
     * Ese ID es fundamental: se usa después para asociar los personajes
     * a esta partida con guardarPersonaje
     * Devuelve -1 si algo falla, para que el código que llama pueda detectarlo.
     */
    public static int crearNuevaPartida(String nombreJugador) {
        String sql = "INSERT INTO partidas (nombre_jugador, turno_actual) VALUES (?, 1)";
        // Los '?' son placeholders. PreparedStatement los sustituye de forma segura,
        // evitando inyección SQL 
        int idPartidaGenerado = -1;

        // TRY-WITH-RESOURCES: cierra Connection y PreparedStatement automáticamente
        // al salir del bloque, haya error o no. Sin esto habría que cerrarlos en finally.
        try (Connection conexion = conectar();
             // RETURN_GENERATED_KEYS le dice a JDBC que queremos recuperar el ID
             // que MySQL asignó automáticamente al hacer el INSERT
             PreparedStatement pstmt = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, nombreJugador); // sustituye el primer '?'
            pstmt.executeUpdate();             // ejecuta el INSERT en la BD

            // getGeneratedKeys() devuelve un ResultSet con el ID generado
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                idPartidaGenerado = rs.getInt(1); // columna 1 = el ID autogenerado
            }
        } catch (SQLException e) { e.printStackTrace(); }

        return idPartidaGenerado;
    }

    /*
     * Inserta un personaje en 'personajes_partida' vinculado a una partida concreta.
     * El parámetro esAliado distingue el bando: true = tu equipo, false = enemigos.
     * Se llama una vez por cada personaje al crear la partida nueva.
     */
    public static void guardarPersonaje(int idPartida, String nombre, String clasePersonaje,
                                        int vidaActual, int vidaMax, int energia, boolean esAliado) {
        String sql = "INSERT INTO personajes_partida (id_partida, nombre, clase, vida_actual, vida_max, energia, es_aliado) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conexion = conectar();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            // Cada setXxx() sustituye un '?' respetando el tipo de dato correcto
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

    // CRUD: READ 

    /*
     * Ejecuta un SELECT sobre 'partidas' y muestra todas las filas por consola.
     * El ResultSet funciona como un cursor: rs.next() avanza fila a fila.
     * rs.getInt("id_partida") :lee la columna por nombre (más claro que por índice).
     */
    public static void listarPartidas() {
        String sql = "SELECT * FROM partidas";
        try (Connection conexion = conectar();
             PreparedStatement pstmt = conexion.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) { // executeQuery() para SELECT (devuelve filas)
                                                    // executeUpdate() es para INSERT/UPDATE/DELETE

            System.out.println("\n=== PARTIDAS GUARDADAS ===");
            boolean hayPartidas = false;

            while (rs.next()) { // cada llamada a next() mueve al siguiente registro
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

    /*
     * Reconstruye objetos Java desde la BD: lee filas de personajes y,
     * según el texto guardado en la columna 'clase', instancia la subclase correcta.
     * Esto es el PATRÓN DE RECONSTRUCCIÓN POLIMÓRFICA: guardamos el tipo como String
     * y al cargar usamos un switch para saber qué clase instanciar.
     * esAliado filtra el bando: true = tu equipo, false = enemigos.
     */
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

                // Switch sobre la columna 'clase' para decidir qué subclase instanciar.
                // El constructor recrea el personaje con sus valores base,
                // y luego los setters sobreescriben con los datos reales de la BD.
                switch (clase) {
                    case "Mercenario": p = new Mercenario(nombre); break;
                    case "Netrunner":  p = new Netrunner(nombre);  break;
                    case "Doctor":     p = new Doctor(nombre);     break;
                    default:
                        System.out.println("Clase desconocida en BD: " + clase);
                        break;
                }

                if (p != null) {
                    // Sobreescribimos los valores por defecto del constructor
                    // con el estado real guardado en la BD (vida y energía actuales)
                    p.setVidaActual(vidaActual);
                    p.setEnergia(energia);
                    lista.add(p);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        return lista;
    }

    // CRUD: UPDATE 

    /*
     * Actualiza vida y energía de un personaje al final de cada ronda (autoguardado).
     * La clave de búsqueda es idPartida + nombre juntos, porque el mismo nombre
     * puede existir en partidas diferentes.
     * executeUpdate() devuelve el número de filas afectadas (no se usa aquí,
     * pero sirve para verificar si el UPDATE encontró la fila o no).
     */
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

    /*
     * Guarda en qué ronda se quedó la partida para poder reanudarla exactamente
     * desde ese punto. Se llama al final de cada ronda junto a actualizarPersonaje().
     */
    public static void actualizarTurno(int idPartida, int turnoActual) {
        String sql = "UPDATE partidas SET turno_actual = ? WHERE id_partida = ?";
        try (Connection conexion = conectar();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, turnoActual);
            pstmt.setInt(2, idPartida);
            pstmt.executeUpdate();

        } catch (SQLException e) { e.printStackTrace(); }
    }

    // CRUD: DELETE 

    /*
     * Borra la partida de la BD. Gracias a ON DELETE CASCADE definido en la BD,
     * al borrar la partida padre se eliminan automáticamente todos sus personajes hijos
     * en 'personajes_partida'. Sin CASCADE habría que borrarlos manualmente antes,
     * o quedarían registros huérfanos (personajes sin partida).
     * executeUpdate() devuelve cuántas filas se borraron:
     * 0 = el ID no existía, >0 = se borró correctamente.
     */
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

    // ── HISTORIAL 

    /*
     * Registra un evento relevante del juego (ataque, muerte, victoria...) en la BD.
     * Cada fila del historial tiene: partida, ronda en la que ocurrió, tipo y descripción.
     * Permite reconstruir cronológicamente qué pasó en la partida.
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
     * Muestra todos los eventos de una partida en orden cronológico.
     * ORDER BY id_historial ASC garantiza el orden de inserción (cronológico),
     * más fiable que ordenar por fecha si varios eventos ocurren en el mismo segundo.
     * printf con %-18s alinea los campos en columnas para que sea legible.
     */
                   
public static void mostrarHistorial(int idPartida) {
            String sql = "SELECT ronda, tipo_evento, descripcion, fecha_hora "
                       + "FROM historial_partida "
                       + "WHERE id_partida = ? "
                       + "ORDER BY id_historial ASC"; // orden de inserción = orden cronológico

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

        // RANKING 

        /*
         * Actualiza el ranking usando INSERT ON DUPLICATE KEY UPDATE.
         * Si el jugador NO existe en la tabla : INSERT (crea su fila).
         * Si el jugador YA existe : UPDATE (suma sus nuevos resultados).
         * Así evitamos tener que hacer primero un SELECT para comprobar si existe.
         * VALUES(victorias) dentro del UPDATE hace referencia al valor que
         * se intentó insertar (0 o 1), no al que ya estaba en la tabla.
         */
        public static void actualizarRanking(String nombreJugador, boolean victoria, int rondasJugadas) {

            int sumaVictorias = victoria ? 1 : 0; // operador ternario: condición ? siTrue : siFalse
            int sumaDerrotas  = victoria ? 0 : 1;

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

                pstmt.setString(1, nombreJugador);
                pstmt.setInt(2, sumaVictorias);
                pstmt.setInt(3, sumaDerrotas);
                pstmt.setInt(4, rondasJugadas);
                pstmt.executeUpdate();

                String resultado = victoria ? "VICTORIA" : "DERROTA";
                System.out.println("\n   [Ranking actualizado → " + nombreJugador
                        + ": " + resultado + " en " + rondasJugadas + " rondas]");

            } catch (SQLException e) { e.printStackTrace(); }
        }

        /*
         * Muestra el ranking ordenado por victorias DESC (más victorias = mejor posición).
         * En caso de empate de victorias, gana quien tiene menos rondas de media
         * (terminó antes, fue más eficiente).
         * ROUND calcula el promedio con 1 decimal directamente en SQL,
         * sin necesidad de traer todos los datos a Java para calcularlo.
         */
        public static void mostrarRanking() {
            String sql = "SELECT nombre_jugador, victorias, derrotas, partidas_jugadas, "
                       + "       ROUND(rondas_totales / partidas_jugadas, 1) AS media_rondas "
                       // AS crea un alias: en el ResultSet se lee como "media_rondas"
                       + "FROM ranking "
                       + "ORDER BY victorias DESC, media_rondas ASC";
                       // DESC = mayor primero (más victorias arriba)
                       // ASC  = menor primero (menos rondas de media = más eficiente)

            try (Connection conexion = conectar();
                 PreparedStatement pstmt = conexion.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

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

                    // Recorta nombres muy largos para que no rompan el formato de la tabla
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
         * Busca el nombre del jugador de una partida para usarlo en actualizarRanking()
         * cuando el combate termina. Devuelve null si el ID no existe,
         * lo que permite al código que llama comprobar si la partida es válida.
         */
        public static String getNombreJugador(int idPartida) {
            String sql = "SELECT nombre_jugador FROM partidas WHERE id_partida = ?";
            try (Connection conexion = conectar();
                 PreparedStatement pstmt = conexion.prepareStatement(sql)) {

                pstmt.setInt(1, idPartida);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    return rs.getString("nombre_jugador");
                }
            } catch (SQLException e) { e.printStackTrace(); }

            return null; // si no encontró ninguna fila con ese ID
        }
    }
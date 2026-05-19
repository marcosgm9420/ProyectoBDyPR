package main;
 
import java.util.Scanner;
import basededatos.GestorBD;
import juego.Juego;
 
public class Main {
 
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int opcion = 0;
        while (opcion != 6) {
            System.out.println("\n=== MENÚ PRINCIPAL RPG ===");
            System.out.println("1. Nueva Partida");
            System.out.println("2. Cargar Partida");
            System.out.println("3. Borrar Partida");
            System.out.println("4. Ver Historial de una Partida");
            System.out.println("5. Ver Ranking Global");
            System.out.println("6. Salir");
            System.out.print("Elige una opción: ");
 
            opcion = sc.nextInt();
            sc.nextLine(); // Limpiar buffer
 
            if (opcion == 1) {
                System.out.print("Introduce tu nombre de jugador: ");
                String nombre = sc.nextLine();
 
                int idPartida = GestorBD.crearNuevaPartida(nombre);
                System.out.println("[OK] Partida creada con ID: " + idPartida);
 
                Juego miJuego = new Juego();
                miJuego.guardarPersonajesIniciales(idPartida);
 
                try {
                    // iniciar ahora devuelve boolean: true = victoria, false = derrota
                    boolean victoria = miJuego.iniciar(idPartida);
                    // getRondasJugadas nos dice cuántas rondas duró el combate.
                    GestorBD.actualizarRanking(nombre, victoria, miJuego.getRondasJugadas());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
 
            } else if (opcion == 2) {
              
                System.out.println("\nPartidas disponibles:");
                GestorBD.listarPartidas();
                System.out.print("Introduce el ID de la partida que quieres cargar: ");
                int idCargar = sc.nextInt();
                sc.nextLine();

                // Recuperamos el nombre del jugador de la BD para poder actualizar
                // el ranking cuando termine, igual que en una partida nueva.
                String nombreJugador = GestorBD.getNombreJugador(idCargar);
 
                Juego miJuego = new Juego();
                miJuego.cargarDesdeBD(idCargar);
 
                try {
                    // Capturamos el resultado igual que en partida nueva
                    boolean victoria = miJuego.iniciar(idCargar);

                    // Actualizamos el ranking solo si conseguimos el nombre
                    if (nombreJugador != null) {
                        GestorBD.actualizarRanking(nombreJugador, victoria, miJuego.getRondasJugadas());
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
 
            } else if (opcion == 3) {
                GestorBD.listarPartidas();
                System.out.print("Introduce el ID de la partida que quieres borrar: ");
                int idBorrar = sc.nextInt();
                sc.nextLine();
                GestorBD.borrarPartida(idBorrar);

            } else if (opcion == 4) {
                System.out.println("\nPartidas disponibles:");
                GestorBD.listarPartidas();
                System.out.print("Introduce el ID de la partida cuyo historial quieres ver: ");
                int idHistorial = sc.nextInt();
                sc.nextLine();
                GestorBD.mostrarHistorial(idHistorial);
            } else if (opcion == 5) {
                // Ver ranking global 
                // No necesita ningún parámetro: muestra todos los jugadores
                GestorBD.mostrarRanking();

            } else if (opcion == 6) {
                System.out.println("Saliendo del juego...");
 
            } else {
                System.out.println("Opción no válida. Elige entre 1 y 6.");
            }
        }
        sc.close();
    }
}
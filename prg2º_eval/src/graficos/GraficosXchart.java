package graficos;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.style.Styler.LegendPosition;
import java.awt.Color;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;


public class GraficosXchart {

 static void mostrarDañoArmas() {

        // Eje X: nombres de las armas
        List<String> armas = Arrays.asList(
            "Katana Vibratoria",
            "Pistola de Reserva",
            "Pistola Taser",
            "Cuchillo Electrico",
            "Rifle Biotico"
        );

        // Eje Y: daño base de cada arma 
        List<Number> daños = Arrays.asList(15, 8, 5, 10, 8);

        // gráfica de barras 
        CategoryChart grafica = new CategoryChartBuilder()
            .width(750)
            .height(500)
            .title("Daño Base de Armas")
            .xAxisTitle("Arma")
            .yAxisTitle("Daño Base (puntos)")
            .build();

        // Personalización visual
        grafica.getStyler().setLegendVisible(false);
        grafica.getStyler().setPlotBackgroundColor(Color.WHITE);
        grafica.getStyler().setChartBackgroundColor(new Color(240, 240, 240));
        grafica.getStyler().setSeriesColors(new Color[]{ new Color(70, 130, 180) }); // azul acero
        grafica.getStyler().setAxisTickLabelsFont(new Font("SansSerif", Font.PLAIN, 11));
        grafica.getStyler().setChartTitleFont(new Font("SansSerif", Font.BOLD, 16));
        grafica.getStyler().setLegendPosition(LegendPosition.InsideNW);

        // Añadimos los datos a la gráfica
        grafica.addSeries("Daño Base", armas, daños);

        // Abrimos la ventana con la gráfica
        new SwingWrapper<>(grafica).displayChart();

        System.out.println("[XChart] Gráfica de daño base mostrada.");
    }


 
    public static void mostrarVidaMaxPersonajes() {

        // Eje X: clases de personaje
        List<String> clases = Arrays.asList(
            "Mercenario",
            "Netrunner",
            "Doctor"
        );

        // Eje Y: vida máxima de cada clase
        List<Number> vidas = Arrays.asList(150, 80, 100);

        // Construimos la gráfica de barras
        CategoryChart grafica = new CategoryChartBuilder()
            .width(600)
            .height(500)
            .title("Vida Máxima por Clase de Personaje")
            .xAxisTitle("Clase")
            .yAxisTitle("Vida Máxima (HP)")
            .build();

        // Personalización visual
        grafica.getStyler().setLegendVisible(false);
        grafica.getStyler().setPlotBackgroundColor(Color.WHITE);
        grafica.getStyler().setChartBackgroundColor(new Color(240, 240, 240));
        grafica.getStyler().setSeriesColors(new Color[]{ new Color(180, 70, 70) }); // rojo
        grafica.getStyler().setAxisTickLabelsFont(new Font("SansSerif", Font.BOLD, 13));
        grafica.getStyler().setChartTitleFont(new Font("SansSerif", Font.BOLD, 16));

        // Añadimos los datos a la gráfica
        grafica.addSeries("Vida Máxima", clases, vidas);

        // Abrimos la ventana con la gráfica
        new SwingWrapper<>(grafica).displayChart();

        System.out.println("[XChart] Gráfica de vida máxima mostrada.");
    }

    public static void mostrarEstadisticas() {
        System.out.println("\n   [XChart] Generando estadísticas del juego...");
        mostrarDañoArmas();
        mostrarVidaMaxPersonajes();
    }
}
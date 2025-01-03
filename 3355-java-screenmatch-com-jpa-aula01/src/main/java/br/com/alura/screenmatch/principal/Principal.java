package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=c6cc0afe";
    private List<DadosSerie> dadosSeries = new ArrayList<>();

    private SerieRepository repositorio;
    private List<Serie> series = new ArrayList<>();

    private Optional<Serie> serieBusca;

    public Principal(SerieRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void exibeMenu() {
       try {
           var opcao = -1;
           while(opcao != 0) {
               var menu = """
                    1 - Buscar séries
                    2 - Buscar episódios
                    3 - Listar séries buscadas
                    4 - Buscar Série por Titulo 
                    5 - Buscar serie por Ator        
                    6 - Top 3 Séries    
                    7 - Buscar Serie por Categoria   
                    8 - Buscar Serie por numero de Temporada
                    9 - Buscar Episodio por Nome
                    10 - Top 5 Episódios por Série
                    11 - Buscar Episodios a partir de uma data
                    0 - Sair                                 
                    """;

               System.out.println(menu);
               opcao = leitura.nextInt();
               leitura.nextLine();

               switch (opcao) {
                   case 1:
                       buscarSerieWeb();
                       break;
                   case 2:
                       buscarEpisodioPorSerie();
                       break;
                   case 3:
                       listarSeriesBuscadas();
                       break;
                   case 4:
                       buscarSeriePorTitulo();
                       break;
                   case 5:
                       buscarSeriesPorAtor();
                       break;
                   case 6:
                       buscarTop3Series();
                       break;
                   case 7:
                       buscarSeriesPorCategoria();
                       break;
                   case 8:
                       buscarSeriesPorNumeroTemporadas();
                       break;
                   case 9:
                       buscarEpisodioPorTrecho();
                       break;
                   case 10:
                       buscarTop5Series();
                       break;
                   case 11:
                       buscarEpPorData();
                       break;
                   case 0:

                       System.out.println("Saindo...");
                       break;
                   default:
                       System.out.println("Opção inválida");
               }
           }

       } catch (InputMismatchException e) {
           System.out.println("Opção inválida. Por favor, insira um número.");
           leitura.nextLine();
       }

    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        //dadosSeries.add(dados);
        repositorio.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();
        System.out.println("Escolha uma série pelo nome");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if(serie.isPresent()) {

            var serieEncontrada = serie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
        } else {
            System.out.println("Série não encontrada!");
        }
    }

    private void listarSeriesBuscadas(){
        series = repositorio.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }
    private void buscarSeriePorTitulo() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        serieBusca = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serieBusca.isPresent()){
            System.out.println("Dados da Serie:" + serieBusca.get());

        } else {
            System.out.println("Serie não encontrada");
        }

    }
    private void buscarSeriesPorAtor() {
        System.out.println("Qual o nome para a busca?");
        var nomeAtor = leitura.nextLine();
        System.out.println("Qual Avaliação o filme precisa ter?");
        var escolhaAvalicao = leitura.nextDouble();

        List<Serie> seriesEncontrdas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, escolhaAvalicao);
        System.out.println("Series em que " + nomeAtor + "  Trabalhou");
        seriesEncontrdas.forEach(s -> System.out.println(s.getTitulo() + " Avaliação: " + s.getAvaliacao()));
    }

    private void buscarTop3Series() {
        List<Serie> topSeries = repositorio.findTop3ByOrderByAvaliacaoDesc();
        topSeries.forEach(s -> System.out.println(s.getTitulo() + " Avalicação: " + s.getAvaliacao()));
    }
    private void buscarSeriesPorCategoria() {
        System.out.println("Digite a categoria da série que deseja buscar:");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);
        System.out.println("Series da Categoria: " + nomeGenero);
        seriesPorCategoria.forEach(System.out::println);
    }

    private void buscarSeriesPorNumeroTemporadas(){
        System.out.println("Digite Quantas temporadas deseja:");
        var numeroTemporada = leitura.nextInt();
        System.out.println("Digite Qual Avalicao deseja: ");
        var avalicaoEscolhida = leitura.nextDouble();
        List<Serie> seriesEncontras = repositorio.seriesPorTemporadaEAvalicao(numeroTemporada, avalicaoEscolhida);
        System.out.println("Series Filtradas");
        seriesEncontras.forEach(System.out::println);
    }

    private void buscarEpisodioPorTrecho() {
        System.out.println("Qual o nome do episodio para a busca?");
        var trechoEp = leitura.nextLine();
        List<Episodio> episodiosEncontrados = repositorio.episodioPorTrecho(trechoEp);
        episodiosEncontrados.forEach(e -> System.out.println("Serie: " + e.getSerie().getTitulo() + "  Nome do episódio:" + e.getTitulo() + "Episodio: " + e.getNumeroEpisodio()));
    }

    private void buscarTop5Series(){
        buscarSeriePorTitulo();

        if (serieBusca.isPresent()){

            Serie serie = serieBusca.get();
            List<Episodio> melhoresEpisodios = repositorio.buscar5MelhorEp(serie);
            melhoresEpisodios.forEach(e -> System.out.println("Serie: " + e.getSerie().getTitulo() + "  Nome do episódio:" + e.getTitulo() + "Episodio: " + e.getNumeroEpisodio() + "Avaliação: " + e.getAvaliacao()));
        }

    }

    private void buscarEpPorData(){
        buscarSeriePorTitulo();
        if (serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            System.out.println("A partir de qual ano voce quer ver os episódios lançados?");
            var dataEscolhida = leitura.nextInt();
            leitura.nextLine();
            List<Episodio> episodiosAno = repositorio.buscarPorData(serie, dataEscolhida);

            episodiosAno.forEach(System.out::println);
        }
    }

}
package com.tinyurl.lookup.constants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock geographic data constants for testing
 * Maps countries to their cities for realistic mock data
 * In production, this would be replaced with IP geolocation (e.g., MaxMind GeoIP2)
 */
public final class MockGeoDataConstants {
    
    private MockGeoDataConstants() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Map of countries to their cities
     * Used for mocking geographic data in click events
     */
    public static final Map<String, List<String>> COUNTRY_CITIES = new HashMap<>();
    
    static {
        COUNTRY_CITIES.put("USA", Arrays.asList(
            "New York", "Los Angeles", "Chicago", "San Francisco", "Boston", "Miami", "Seattle", "Houston"
        ));
        COUNTRY_CITIES.put("UK", Arrays.asList(
            "London", "Manchester", "Birmingham", "Liverpool", "Leeds", "Edinburgh", "Glasgow"
        ));
        COUNTRY_CITIES.put("Canada", Arrays.asList(
            "Toronto", "Vancouver", "Montreal", "Calgary", "Ottawa", "Edmonton", "Winnipeg"
        ));
        COUNTRY_CITIES.put("Germany", Arrays.asList(
            "Berlin", "Munich", "Hamburg", "Frankfurt", "Cologne", "Stuttgart", "Düsseldorf"
        ));
        COUNTRY_CITIES.put("France", Arrays.asList(
            "Paris", "Lyon", "Marseille", "Toulouse", "Nice", "Nantes", "Strasbourg"
        ));
        COUNTRY_CITIES.put("Italy", Arrays.asList(
            "Rome", "Milan", "Naples", "Turin", "Palermo", "Genoa", "Bologna"
        ));
        COUNTRY_CITIES.put("Spain", Arrays.asList(
            "Madrid", "Barcelona", "Valencia", "Seville", "Bilbao", "Málaga", "Murcia"
        ));
        COUNTRY_CITIES.put("Netherlands", Arrays.asList(
            "Amsterdam", "Rotterdam", "The Hague", "Utrecht", "Eindhoven", "Groningen"
        ));
        COUNTRY_CITIES.put("Australia", Arrays.asList(
            "Sydney", "Melbourne", "Brisbane", "Perth", "Adelaide", "Gold Coast", "Canberra"
        ));
        COUNTRY_CITIES.put("Japan", Arrays.asList(
            "Tokyo", "Osaka", "Yokohama", "Kyoto", "Sapporo", "Fukuoka", "Hiroshima"
        ));
        COUNTRY_CITIES.put("China", Arrays.asList(
            "Beijing", "Shanghai", "Guangzhou", "Shenzhen", "Chengdu", "Hangzhou", "Wuhan"
        ));
        COUNTRY_CITIES.put("India", Arrays.asList(
            "Mumbai", "Delhi", "Bangalore", "Hyderabad", "Chennai", "Kolkata", "Pune"
        ));
        COUNTRY_CITIES.put("Brazil", Arrays.asList(
            "São Paulo", "Rio de Janeiro", "Brasília", "Salvador", "Fortaleza", "Belo Horizonte"
        ));
        COUNTRY_CITIES.put("Mexico", Arrays.asList(
            "Mexico City", "Guadalajara", "Monterrey", "Puebla", "Tijuana", "León"
        ));
        COUNTRY_CITIES.put("Argentina", Arrays.asList(
            "Buenos Aires", "Córdoba", "Rosario", "Mendoza", "Tucumán", "La Plata"
        ));
        COUNTRY_CITIES.put("South Korea", Arrays.asList(
            "Seoul", "Busan", "Incheon", "Daegu", "Daejeon", "Gwangju", "Ulsan"
        ));
        COUNTRY_CITIES.put("Singapore", Arrays.asList(
            "Singapore"
        ));
        COUNTRY_CITIES.put("Sweden", Arrays.asList(
            "Stockholm", "Gothenburg", "Malmö", "Uppsala", "Västerås", "Örebro"
        ));
        COUNTRY_CITIES.put("Norway", Arrays.asList(
            "Oslo", "Bergen", "Trondheim", "Stavanger", "Bærum", "Kristiansand"
        ));
        COUNTRY_CITIES.put("Denmark", Arrays.asList(
            "Copenhagen", "Aarhus", "Odense", "Aalborg", "Esbjerg", "Randers"
        ));
        COUNTRY_CITIES.put("Poland", Arrays.asList(
            "Warsaw", "Kraków", "Łódź", "Wrocław", "Poznań", "Gdańsk"
        ));
        COUNTRY_CITIES.put("Russia", Arrays.asList(
            "Moscow", "Saint Petersburg", "Novosibirsk", "Yekaterinburg", "Kazan", "Nizhny Novgorod"
        ));
        COUNTRY_CITIES.put("Turkey", Arrays.asList(
            "Istanbul", "Ankara", "İzmir", "Bursa", "Antalya", "Adana"
        ));
        COUNTRY_CITIES.put("South Africa", Arrays.asList(
            "Cape Town", "Johannesburg", "Durban", "Pretoria", "Port Elizabeth", "Bloemfontein"
        ));
        COUNTRY_CITIES.put("Egypt", Arrays.asList(
            "Cairo", "Alexandria", "Giza", "Shubra El Kheima", "Port Said", "Suez"
        ));
        COUNTRY_CITIES.put("Saudi Arabia", Arrays.asList(
            "Riyadh", "Jeddah", "Mecca", "Medina", "Dammam", "Khobar"
        ));
        COUNTRY_CITIES.put("UAE", Arrays.asList(
            "Dubai", "Abu Dhabi", "Sharjah", "Al Ain", "Ajman", "Ras Al Khaimah"
        ));
    }
    
    /**
     * Get a random country from the available countries
     */
    public static String getRandomCountry() {
        List<String> countries = List.copyOf(COUNTRY_CITIES.keySet());
        return countries.get((int) (Math.random() * countries.size()));
    }
    
    /**
     * Get a random city for a given country
     */
    public static String getRandomCity(String country) {
        List<String> cities = COUNTRY_CITIES.get(country);
        if (cities == null || cities.isEmpty()) {
            return "Unknown";
        }
        return cities.get((int) (Math.random() * cities.size()));
    }
}


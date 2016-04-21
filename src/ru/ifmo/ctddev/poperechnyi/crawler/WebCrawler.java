package ru.ifmo.ctddev.poperechnyi.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;


public class WebCrawler implements Crawler {
    private Downloader downloader;
    private int downloaders;
    private int extraxtors;
    private int perHost;
    ExecutorService downloadExecutor;
    ExecutorService extractExecutor;
    private int maxDepth;
    List<Future<?>> tasks;

    public WebCrawler(Downloader downloader, int downloaders, int extraxtors, int perHost) {
        this.downloader = downloader;
        this.downloaders = downloaders;
        this.extraxtors = extraxtors;
        this.perHost = perHost;
        this.downloadExecutor = Executors.newFixedThreadPool(downloaders);
        this.extractExecutor = Executors.newFixedThreadPool(extraxtors);
    }


    private void extractLinks(
            ConcurrentHashMap<String, IOException> errors,
            Set<String> results,
            int depth,
            Document doc,
            String url) {
        try {
            List<String> links = doc.extractLinks();
            links.stream().filter(link -> !results.contains(link)).forEach(link -> {
                results.add(link);
                tasks.add(downloadExecutor.submit(() -> downloadPage(errors, results, depth + 1, link)));
                notifyAll();
            });
        } catch (IOException e) {
            errors.put(url, e);
        }
    }

    private void downloadPage(
            ConcurrentHashMap<String, IOException> errors,
            Set<String> results,
            int depth,
            String url) {
        if (depth > maxDepth) {
            return;
        }
        try {
            String host = URLUtils.getHost(url);
            Document doc = downloader.download(host);
            tasks.add(extractExecutor.submit(() -> extractLinks(errors, results, depth, doc, host)));
            notifyAll();
        } catch (IOException e) {
            errors.put(url, e);
        }
    }

    @Override
    public Result download(String url, int depth) {
        this.maxDepth = depth;
        ConcurrentHashMap<String, IOException> errors = new ConcurrentHashMap<>();
        Set<String> results = ConcurrentHashMap.newKeySet();
        tasks = new ArrayList<>();
        try {
            String host = URLUtils.getHost(url);
            try {
                Document doc = downloader.download(url);
                results.add(url);
                tasks.add(extractExecutor.submit(() -> extractLinks(errors, results, 1, doc, url)));
            } catch (IOException e) {
                errors.put(url, e);
            }
        } catch (MalformedURLException e) {
            errors.put(url, e);
        }
        List<String> res = new ArrayList<>(results);
        while (true) {
            for (Future<?> task : tasks) {
                try {
                    synchronized (task.get()) {
                        if (!task.isDone()) {
                            task.get().wait();
                            break;
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            break;
        }
        System.out.println(res.size());
        return new Result(res, errors);
    }

    @Override
    public void close() {
        if (!downloadExecutor.isShutdown()) {
            downloadExecutor.shutdown();
        }
        if (!extractExecutor.isShutdown()) {
            extractExecutor.shutdown();
        }
    }
}

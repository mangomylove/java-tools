/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.datagrid;

import com.zhaoliang.ignite.Client;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.lang.IgniteRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * This example demonstrates the simplest code that populates the distributed cache
 * and co-locates simple closure execution with each key. The goal of this particular
 * example is to provide the simplest code example of this logic.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: {@code 'ignite.{sh|bat} examples/config/example-ignite.xml'}.
 * <p>
 * Alternatively you can run {@link } in another JVM which will
 * start node with {@code examples/config/example-ignite.xml} configuration.
 */
public final class CacheAffinityExample {
    /**
     * Cache name.
     */
    private static final String CACHE_NAME = CacheAffinityExample.class.getSimpleName();

    /**
     * Number of keys.
     */
    private static final int KEY_CNT = 20;

    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws IgniteException If example execution failed.
     */
    public static void main(String[] args) throws IgniteException {
        // Start Ignite in client mode.
//        Ignite ignite = Ignition.start(new IgniteConfiguration().setClientMode(true).setPeerClassLoadingEnabled(true));
        Ignite ignite = Client.getIgnite2();
        System.out.println();
        System.out.println(">>> Cache affinity example started.");

        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>();

        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setName(CACHE_NAME);

        // Auto-close cache at the end of the example.
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);
        for (int i = 0; i < KEY_CNT; i++) {
            cache.put(i, Integer.toString(i));
        }

        // Co-locates jobs with data using IgniteCompute.affinityRun(...) method.
        visitUsingAffinityRun();

        // Co-locates jobs with data using IgniteCluster.mapKeysToNodes(...) method.
        visitUsingMapKeysToNodes();

        // Distributed cache could be removed from cluster only by #destroyCache() call.
        ignite.destroyCache(CACHE_NAME);
        ignite.close();
    }


    /**
     * Collocates jobs with keys they need to work on using
     * {@link IgniteCompute#affinityRun(String, Object, IgniteRunnable)} method.
     */
    private static void visitUsingAffinityRun() {
        Ignite ignite = Ignition.ignite();

        final IgniteCache<Integer, String> cache = ignite.cache(CACHE_NAME);

        for (int i = 0; i < KEY_CNT; i++) {
            int key = i;

            // This runnable will execute on the remote node where
            // data with the given key is located. Since it will be co-located
            // we can use local 'peek' operation safely.
//            ignite.compute().affinityRun(CACHE_NAME, key,
//                                         () -> System.out.println("Co-located using affinityRun [key= " + key + ", value=" + cache.localPeek(key) + ']'));
            String affinityCall = ignite.compute().affinityCall(CACHE_NAME, key, (IgniteCallable<String>) () -> cache.localPeek(key) + "-11111111111111");
            System.out.println(">>> " + affinityCall);
        }
    }

    /**
     * Collocates jobs with keys they need to work on using {@link Affinity#mapKeysToNodes(Collection)}
     * method. The difference from {@code affinityRun(...)} method is that here we process multiple keys
     * in a single job.
     */
    private static void visitUsingMapKeysToNodes() {
        final Ignite ignite = Ignition.ignite();

        Collection<Integer> keys = new ArrayList<>(KEY_CNT);

        for (int i = 0; i < KEY_CNT; i++) {
            keys.add(i);
        }

        // Map all keys to nodes.
        Map<ClusterNode, Collection<Integer>> mappings = ignite.<Integer>affinity(CACHE_NAME).mapKeysToNodes(keys);

        for (Map.Entry<ClusterNode, Collection<Integer>> mapping : mappings.entrySet()) {
            ClusterNode node = mapping.getKey();
            System.out.println(node.addresses());

            final Collection<Integer> mappedKeys = mapping.getValue();

            if (node != null) {
                // Bring computations to the nodes where the data resides (i.e. collocation).
                ignite.compute(ignite.cluster().forNode(node)).run(() -> {
                    IgniteCache<Integer, String> cache = ignite.cache(CACHE_NAME);

                    // Peek is a local memory lookup, however, value should never be 'null'
                    // as we are co-located with node that has a given key.
                    for (Integer key : mappedKeys) {
                        System.out.println("Co-located using mapKeysToNodes [key= " + key +
                                               ", value=" + cache.localPeek(key) + ']');
                    }
                });
            }
        }
    }
}

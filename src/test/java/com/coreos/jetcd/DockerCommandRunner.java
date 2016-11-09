/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2016.                            (c) 2016.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 *  NRC disclaims any warranties,        Le CNRC dénie toute garantie
 *  expressed, implied, or               énoncée, implicite ou légale,
 *  statutory, of any kind with          de quelque nature que ce
 *  respect to the software,             soit, concernant le logiciel,
 *  including without limitation         y compris sans restriction
 *  any warranty of merchantability      toute garantie de valeur
 *  or fitness for a particular          marchande ou de pertinence
 *  purpose. NRC shall not be            pour un usage particulier.
 *  liable in any event for any          Le CNRC ne pourra en aucun cas
 *  damages, whether direct or           être tenu responsable de tout
 *  indirect, special or general,        dommage, direct ou indirect,
 *  consequential or incidental,         particulier ou général,
 *  arising from the use of the          accessoire ou fortuit, résultant
 *  software.  Neither the name          de l'utilisation du logiciel. Ni
 *  of the National Research             le nom du Conseil National de
 *  Council of Canada nor the            Recherches du Canada ni les noms
 *  names of its contributors may        de ses  participants ne peuvent
 *  be used to endorse or promote        être utilisés pour approuver ou
 *  products derived from this           promouvoir les produits dérivés
 *  software without specific prior      de ce logiciel sans autorisation
 *  written permission.                  préalable et particulière
 *                                       par écrit.
 *
 *  This file is part of the             Ce fichier fait partie du projet
 *  OpenCADC project.                    OpenCADC.
 *
 *  OpenCADC is free software:           OpenCADC est un logiciel libre ;
 *  you can redistribute it and/or       vous pouvez le redistribuer ou le
 *  modify it under the terms of         modifier suivant les termes de
 *  the GNU Affero General Public        la “GNU Affero General Public
 *  License as published by the          License” telle que publiée
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (à votre gré)
 *  any later version.                   toute version ultérieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribué
 *  hope that it will be useful,         dans l’espoir qu’il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans même la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
 *  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           Générale Publique GNU Affero
 *  more details.                        pour plus de détails.
 *
 *  You should have received             Vous devriez avoir reçu une
 *  a copy of the GNU Affero             copie de la Licence Générale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                       <http://www.gnu.org/licenses/>.
 *
 *
 ************************************************************************
 */

package com.coreos.jetcd;

import com.coreos.jetcd.integration.DockerContainerInstance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


public class DockerCommandRunner
{
    private static final Logger LOGGER = Logger.getLogger(
            DockerCommandRunner.class.getName());
    private static final int DOCKER_PROCESS_COMPLETE = 0;
    private static final String DEFAULT_DOCKER_IMAGE_NAME =
            "quay.io/coreos/etcd";
    private static final String DOCKER_IMAGE_NAME_PROPERTY_KEY =
            "ETCD_DOCKER_IMAGE";


    public InputStream runDockerCommand(final String... command)
            throws Exception
    {
        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        final Process process = processBuilder.start();

        final int exitCode = process.waitFor();

        if (exitCode == DOCKER_PROCESS_COMPLETE)
        {
            return process.getInputStream();
        }
        else
        {
            final InputStream errorStream = process.getErrorStream();

            throw new RuntimeException(
                    String.format("Unable to execute \n'%s'\n due to (%d): %s.",
                                  Arrays.toString(command),
                                  exitCode, readMessage(errorStream)));
        }
    }

    private void pullLatestImage() throws Exception
    {
        LOGGER.info("\nPulling latest image...\n");
        final String dockerImageName = getETCDDockerImageName();
        final String[] dockerPull = new String[] {
                "docker", "pull", dockerImageName
        };

        runDockerCommand(dockerPull);
    }

    private String getETCDDockerImageName()
    {
        return System.getProperty(DOCKER_IMAGE_NAME_PROPERTY_KEY,
                                  DEFAULT_DOCKER_IMAGE_NAME);
    }

    private void ensureNetworkExists(final String networkName) throws Exception
    {
        final InputStream inputStream =
                runDockerCommand("docker", "network", "ls", "-q", "--filter",
                                 "name=" + networkName);

        final String output = readMessage(inputStream);

        if (output.trim().equals(""))
        {
            LOGGER.info("Creating network " + networkName);
            runDockerCommand("docker", "network", "create", networkName);
        }
    }

    /**
     * Start a single instance.
     * @return          The running instance.
     * @throws Exception    Any errors starting it up.
     */
    DockerContainerInstance run() throws Exception
    {
        return run(1)[0];
    }

    /**
     * Run a cluster of name:host instances.
     *
     * @param count             The number of items in the cluster.
     * @return                  Array in the cluster.
     * @throws Exception    Any errors starting it up.
     */
    DockerContainerInstance[] run(final int count) throws Exception
    {
        final String networkName = "etcd_test";

        pullLatestImage();
        ensureNetworkExists(networkName);

        final DockerContainerInstance[] instances =
                new DockerContainerInstance[count];
        final Map<String, String> nameHosts = new HashMap<>();

        final int startIndex = (count == 1) ? 0 : 1;
        final int endIndex = (count == 1) ? count : (count + 1);

        for (int i = startIndex; i < endIndex; i++)
        {
            nameHosts.put("etcd" + i, "localhost");
        }

        final String clusterString = getClusterString(nameHosts);

        int currIndex = 0;
        for (final Map.Entry<String, String> entry : nameHosts.entrySet())
        {
            final String name = entry.getKey();
            final String host = entry.getValue();
            final String portPrefix = (count == 1) ? "" : ((currIndex + 1) + "");

            final String[] command = new String[] {
                    "docker", "run", "-d", "--name", name,
                    "--net=" + networkName,
                    //                "-v /usr/share/ca-certificates/:/etc/ssl/certs",
                    "-p", portPrefix + "4001:4001", "-p",
                    portPrefix + "2380:2380", "-p", portPrefix + "2379:2379",
                    getETCDDockerImageName(), "etcd",
                    "-name", name,
                    "-advertise-client-urls",
                    "http://" + name + ":2379,http://" + name + ":4001",
                    "-listen-client-urls",
                    "http://" + name + ":2379,http://" + name + ":4001",
                    "-initial-advertise-peer-urls", "http://" + name + ":2380",
                    "-listen-peer-urls", "http://" + name + ":2380",
                    "-initial-cluster-token", "etcd-cluster-1",
                    "-initial-cluster", clusterString,
                    "-initial-cluster-state", "new"
            };

            final InputStream inputStream = runDockerCommand(command);

            // Allow each container to start up.  Not sleeping here causes the
            // yet-to-be ready cluster to fail adding a node.
            Thread.sleep(2500L);

            instances[currIndex++] =
                    new DockerContainerInstance(this,
                                                readMessage(inputStream).toCharArray(),
                                                host + ":" + portPrefix
                                                + "2379");
        }

        return instances;
    }

    private String getClusterString(final Map<String, String> nameHosts)
    {
        final StringBuilder clusterString = new StringBuilder();
        for (final Map.Entry<String, String> entry : nameHosts.entrySet())
        {
            final String name = entry.getKey();
            clusterString.append(name).append("=http://").append(name)
                    .append(":2380,");
        }

        clusterString.deleteCharAt(clusterString.lastIndexOf(","));

        return clusterString.toString();
    }

    public String readMessage(final InputStream inputStream) throws IOException
    {
        final BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(inputStream));
        final StringBuilder message = new StringBuilder();
        String line;

        while ((line = bufferedReader.readLine()) != null)
        {
            message.append(line).append("\n");
        }

        bufferedReader.close();
        return message.toString().trim();
    }
}

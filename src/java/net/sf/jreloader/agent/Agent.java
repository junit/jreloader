/*
 * Copyright 2006 Antonio S. R. Gomes
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package net.sf.jreloader.agent;

import java.lang.instrument.Instrumentation;

/**
 * Class responsible for the intialization of the agent.
 *
 * @author Antonio S. R. Gomes
 */
public class Agent {

    static Transformer t;
    static Instrumentation inst;

    /**
     * Premain method called before the target application is initialized.
     *
     * @param args command line argument passed via <code>-javaagent</code>
     * @param inst instance of the instrumentation service
     */
    public static void premain(String args, Instrumentation inst) {

        System.err.println("+---------------------------------------------------+");
        System.err.println("| JReloader Agent " + String.format("%-34s", Version.VERSION) + "|");
        System.err.println("| Copyright 2008,2009 Antonio S. R. Gomes           |");
        System.err.println("| See LICENSE-2.0.txt details                       |");
        System.err.println("+---------------------------------------------------+");

        Agent.inst = inst;
        t = new Transformer();
        inst.addTransformer(t);
    }

}

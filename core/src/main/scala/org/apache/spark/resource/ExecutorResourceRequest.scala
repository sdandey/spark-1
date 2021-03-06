/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.resource

import scala.collection.mutable

import org.apache.spark.resource.ResourceUtils.RESOURCE_DOT

/**
 * An Executor resource request. This is used in conjunction with the ResourceProfile to
 * programmatically specify the resources needed for an RDD that will be applied at the
 * stage level.
 *
 * This is used to specify what the resource requirements are for an Executor and how
 * Spark can find out specific details about those resources. Not all the parameters are
 * required for every resource type. The resources names supported
 * correspond to the regular Spark configs with the prefix removed. For instance overhead
 * memory in this api is memoryOverhead, which is spark.executor.memoryOverhead with
 * spark.executor removed. Resources like GPUs are resource.gpu
 * (spark configs spark.executor.resource.gpu.*). The amount, discoveryScript, and vendor
 * parameters for resources are all the same parameters a user would specify through the
 * configs: spark.executor.resource.{resourceName}.{amount, discoveryScript, vendor}.
 *
 * For instance, a user wants to allocate an Executor with GPU resources on YARN. The user has
 * to specify the resource name (resource.gpu), the amount or number of GPUs per Executor,
 * the discovery script would be specified so that when the Executor starts up it can
 * discovery what GPU addresses are available for it to use because YARN doesn't tell
 * Spark that, then vendor would not be used because its specific for Kubernetes.
 *
 * See the configuration and cluster specific docs for more details.
 *
 * Use ExecutorResourceRequests class as a convenience API.
 *
 * @param resourceName Name of the resource
 * @param amount Amount requesting
 * @param discoveryScript Optional script used to discover the resources. This is required on some
 *                        cluster managers that don't tell Spark the addresses of the resources
 *                        allocated. The script runs on Executors startup to discover the addresses
 *                        of the resources available.
 * @param vendor Optional vendor, required for some cluster managers
 *
 * This api is currently private until the rest of the pieces are in place and then it
 * will become public.
 */
private[spark] class ExecutorResourceRequest(
    val resourceName: String,
    val amount: Long,
    val discoveryScript: String = "",
    val vendor: String = "") extends Serializable {

  // A list of allowed Spark internal resources. Custom resources (spark.executor.resource.*)
  // like GPUs/FPGAs are also allowed, see the check below.
  private val allowedExecutorResources = mutable.HashSet[String](
    ResourceProfile.MEMORY,
    ResourceProfile.OVERHEAD_MEM,
    ResourceProfile.PYSPARK_MEM,
    ResourceProfile.CORES)

  if (!allowedExecutorResources.contains(resourceName) && !resourceName.startsWith(RESOURCE_DOT)) {
    throw new IllegalArgumentException(s"Executor resource not allowed: $resourceName")
  }
}
